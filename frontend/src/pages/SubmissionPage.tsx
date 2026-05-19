import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { submissionApi } from '../api/endpoints';
import type { Submission, ProgressUpdate } from '../types';
import toast from 'react-hot-toast';
import Editor from '@monaco-editor/react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  ArrowLeft, Download, AlertTriangle, AlertCircle, Info, CheckCircle,
  FileCode2, Gauge, Shield, Brain, Loader
} from 'lucide-react';

export default function SubmissionPage() {
  const { id } = useParams<{ id: string }>();
  const [submission, setSubmission] = useState<Submission | null>(null);
  const [sourceCode, setSourceCode] = useState('');
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'findings' | 'metrics' | 'plagiarism'>('findings');
  const stompRef = useRef<Client | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    Promise.all([
      submissionApi.get(Number(id)),
      submissionApi.getSource(Number(id)),
    ]).then(([subRes, srcRes]) => {
      setSubmission(subRes.data.data);
      setSourceCode(srcRes.data.data);
    }).catch(() => toast.error('Failed to load submission'))
      .finally(() => setLoading(false));
  }, [id]);

  // WebSocket for real-time progress
  useEffect(() => {
    if (!id || submission?.status === 'COMPLETED' || submission?.status === 'FAILED') return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/progress/${id}`, (msg) => {
          const update: ProgressUpdate = JSON.parse(msg.body);
          setSubmission((prev) => prev ? {
            ...prev, currentStage: update.stage,
            progressPercent: update.progressPercent, status: update.status,
          } : prev);

          if (update.status === 'COMPLETED') {
            submissionApi.get(Number(id)).then((res) => setSubmission(res.data.data));
          }
        });
      },
    });

    client.activate();
    stompRef.current = client;

    const refreshInterval = setInterval(() => {
      submissionApi.get(Number(id)).then((res) => {
        setSubmission(res.data.data);
        if (res.data.data.status === 'COMPLETED' || res.data.data.status === 'FAILED') {
          clearInterval(refreshInterval);
        }
      });
    }, 3000);

    return () => {
      client.deactivate();
      clearInterval(refreshInterval);
    };
  }, [id, submission?.status]);

  const handleDownloadReport = async () => {
    if (!id) return;
    try {
      const res = await submissionApi.downloadReport(Number(id));
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = `review-report-${id}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
      toast.success('Report downloaded!');
    } catch {
      toast.error('Failed to download report');
    }
  };

  const langMap: Record<string, string> = {
    Java: 'java', Python: 'python', JavaScript: 'javascript', TypeScript: 'typescript',
    'C++': 'cpp', C: 'c', Go: 'go', Rust: 'rust', Ruby: 'ruby', PHP: 'php',
    Swift: 'swift', Kotlin: 'kotlin',
  };

  const severityIcon = (s: string) => {
    switch (s) {
      case 'CRITICAL': return <AlertCircle className="w-4 h-4 text-red-400" />;
      case 'WARNING': return <AlertTriangle className="w-4 h-4 text-yellow-400" />;
      default: return <Info className="w-4 h-4 text-blue-400" />;
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center h-[60vh]">
      <div className="w-10 h-10 border-3 border-primary-500/30 border-t-primary-500 rounded-full animate-spin" />
    </div>
  );

  if (!submission) return <p className="text-center text-surface-300/40">Submission not found</p>;

  const isProcessing = submission.status !== 'COMPLETED' && submission.status !== 'FAILED';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="p-2 rounded-xl hover:bg-white/10">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-3">
              <FileCode2 className="w-6 h-6 text-primary-400" />
              {submission.originalFileName}
            </h1>
            <div className="flex items-center gap-3 mt-1 text-sm text-surface-300/50">
              <span>{submission.language}</span>
              <span>•</span>
              <span>{(submission.fileSize / 1024).toFixed(1)} KB</span>
              <span>•</span>
              <span>{new Date(submission.createdAt).toLocaleString()}</span>
            </div>
          </div>
        </div>
        {submission.status === 'COMPLETED' && (
          <button onClick={handleDownloadReport} className="btn-primary flex items-center gap-2">
            <Download className="w-4 h-4" /> Download Report
          </button>
        )}
      </div>

      {/* Progress Bar (while processing) */}
      {isProcessing && (
        <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}
          className="glass-card p-6">
          <div className="flex items-center gap-3 mb-3">
            <Loader className="w-5 h-5 text-primary-400 animate-spin" />
            <span className="font-medium">{submission.currentStage || 'Processing...'}</span>
            <span className="text-sm text-surface-300/50 ml-auto">{submission.progressPercent}%</span>
          </div>
          <div className="h-2.5 bg-white/5 rounded-full overflow-hidden">
            <motion.div className="h-full bg-gradient-to-r from-primary-600 to-primary-400 rounded-full"
              animate={{ width: `${submission.progressPercent}%` }} transition={{ duration: 0.5 }} />
          </div>
        </motion.div>
      )}

      {/* Score Overview */}
      {submission.review && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="stat-card items-center">
            <div className={`text-4xl font-bold ${
              submission.review.overallScore >= 80 ? 'text-emerald-400' : submission.review.overallScore >= 50 ? 'text-yellow-400' : 'text-red-400'
            }`}>{submission.review.overallScore}</div>
            <p className="text-xs text-surface-300/60 uppercase">Overall Score</p>
          </motion.div>
          <div className="stat-card items-center">
            <div className="text-3xl font-bold text-red-400">{submission.review.criticalCount}</div>
            <p className="text-xs text-surface-300/60 uppercase">Critical</p>
          </div>
          <div className="stat-card items-center">
            <div className="text-3xl font-bold text-yellow-400">{submission.review.warningCount}</div>
            <p className="text-xs text-surface-300/60 uppercase">Warnings</p>
          </div>
          <div className="stat-card items-center">
            <div className="text-3xl font-bold text-blue-400">{submission.review.infoCount}</div>
            <p className="text-xs text-surface-300/60 uppercase">Info</p>
          </div>
        </div>
      )}

      {/* Code Editor + Tabs */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Monaco Editor */}
        <div className="glass-card overflow-hidden">
          <div className="px-4 py-3 border-b border-white/5 flex items-center gap-2">
            <FileCode2 className="w-4 h-4 text-primary-400" />
            <span className="text-sm font-medium">Source Code</span>
          </div>
          <Editor
            height="500px"
            language={langMap[submission.language] || 'plaintext'}
            value={sourceCode}
            theme="vs-dark"
            options={{ readOnly: true, minimap: { enabled: false }, fontSize: 13, scrollBeyondLastLine: false, padding: { top: 12 } }}
          />
        </div>

        {/* Analysis Tabs */}
        <div className="glass-card overflow-hidden flex flex-col">
          <div className="flex border-b border-white/5">
            {[
              { key: 'findings', label: 'Findings', icon: AlertTriangle, count: submission.review?.totalIssues },
              { key: 'metrics', label: 'Metrics', icon: Gauge },
              { key: 'plagiarism', label: 'Plagiarism', icon: Shield },
            ].map((tab) => (
              <button key={tab.key}
                onClick={() => setActiveTab(tab.key as any)}
                className={`flex items-center gap-2 px-5 py-3 text-sm font-medium transition-all border-b-2 ${
                  activeTab === tab.key ? 'border-primary-500 text-primary-400' : 'border-transparent text-surface-300/50 hover:text-white'
                }`}>
                <tab.icon className="w-4 h-4" />
                {tab.label}
                {tab.count !== undefined && <span className="text-xs bg-white/10 px-1.5 py-0.5 rounded-full">{tab.count}</span>}
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-y-auto p-4 max-h-[460px]">
            {activeTab === 'findings' && submission.review && (
              <div className="space-y-3">
                {submission.review.findings.length === 0 ? (
                  <div className="text-center py-12">
                    <CheckCircle className="w-10 h-10 text-emerald-400 mx-auto mb-3" />
                    <p className="text-surface-300/60">No issues found!</p>
                  </div>
                ) : submission.review.findings.map((f) => (
                  <div key={f.id} className="p-4 rounded-xl bg-white/5 border border-white/5 space-y-2">
                    <div className="flex items-start gap-2">
                      {severityIcon(f.severity)}
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-sm">{f.title}</span>
                          <span className={`text-[10px] px-1.5 py-0.5 rounded-full ${
                            f.source === 'AI' ? 'bg-violet-500/20 text-violet-400' : 'bg-blue-500/20 text-blue-400'
                          }`}>{f.source}</span>
                        </div>
                        {f.lineNumber && <span className="text-xs text-surface-300/40">Line {f.lineNumber}</span>}
                      </div>
                      <span className={f.severity === 'CRITICAL' ? 'badge-critical' : f.severity === 'WARNING' ? 'badge-warning' : 'badge-info'}>
                        {f.severity}
                      </span>
                    </div>
                    <p className="text-sm text-surface-300/70">{f.description}</p>
                    {f.recommendation && (
                      <div className="text-sm text-emerald-400/80 bg-emerald-500/5 p-2 rounded-lg border border-emerald-500/10">
                        💡 {f.recommendation}
                      </div>
                    )}
                    {f.fixedCode && (
                      <pre className="text-xs bg-surface-900 p-3 rounded-lg overflow-x-auto border border-white/5">
                        <code>{f.fixedCode}</code>
                      </pre>
                    )}
                  </div>
                ))}
              </div>
            )}

            {activeTab === 'metrics' && submission.metrics && (
              <div className="grid grid-cols-2 gap-3">
                {[
                  { label: 'Lines of Code', value: submission.metrics.linesOfCode },
                  { label: 'Code Lines', value: submission.metrics.codeLines },
                  { label: 'Comment Lines', value: submission.metrics.commentLines },
                  { label: 'Blank Lines', value: submission.metrics.blankLines },
                  { label: 'Comment Ratio', value: `${submission.metrics.commentRatio}%` },
                  { label: 'Cyclomatic Complexity', value: submission.metrics.cyclomaticComplexity },
                  { label: 'Maintainability', value: `${submission.metrics.maintainabilityIndex}%` },
                  { label: 'Methods', value: submission.metrics.numberOfMethods },
                  { label: 'Classes', value: submission.metrics.numberOfClasses },
                  { label: 'Avg Method Length', value: submission.metrics.averageMethodLength },
                  { label: 'Max Nesting', value: submission.metrics.maxNestingDepth },
                  { label: 'Imports', value: submission.metrics.numberOfImports },
                ].map((m) => (
                  <div key={m.label} className="p-3 rounded-xl bg-white/5 border border-white/5">
                    <p className="text-lg font-bold">{m.value}</p>
                    <p className="text-xs text-surface-300/50">{m.label}</p>
                  </div>
                ))}
              </div>
            )}

            {activeTab === 'plagiarism' && (
              submission.plagiarism ? (
                <div className="space-y-4">
                  <div className="text-center py-6">
                    <div className={`text-5xl font-bold ${
                      submission.plagiarism.similarityPercentage < 20 ? 'text-emerald-400' :
                      submission.plagiarism.similarityPercentage < 40 ? 'text-yellow-400' : 'text-red-400'
                    }`}>{submission.plagiarism.similarityPercentage}%</div>
                    <p className="text-sm text-surface-300/60 mt-1">Similarity</p>
                    {submission.plagiarism.flagged && (
                      <span className="badge-critical mt-2 inline-block">⚠️ Flagged for review</span>
                    )}
                  </div>
                  {submission.plagiarism.comparedFileName && (
                    <p className="text-sm text-surface-300/60 text-center">
                      Compared with: <span className="text-white">{submission.plagiarism.comparedFileName}</span>
                    </p>
                  )}
                </div>
              ) : (
                <div className="text-center py-12">
                  <Shield className="w-10 h-10 text-surface-300/20 mx-auto mb-3" />
                  <p className="text-surface-300/40">No plagiarism data available</p>
                </div>
              )
            )}

            {activeTab === 'findings' && !submission.review && (
              <div className="text-center py-12">
                <Brain className="w-10 h-10 text-surface-300/20 mx-auto mb-3" />
                <p className="text-surface-300/40">{isProcessing ? 'Analysis in progress...' : 'No review data'}</p>
              </div>
            )}

            {activeTab === 'metrics' && !submission.metrics && (
              <div className="text-center py-12">
                <Gauge className="w-10 h-10 text-surface-300/20 mx-auto mb-3" />
                <p className="text-surface-300/40">{isProcessing ? 'Calculating metrics...' : 'No metrics data'}</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
