import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { projectApi, submissionApi } from '../api/endpoints';
import type { Project, Submission } from '../types';
import toast from 'react-hot-toast';
import { useDropzone } from 'react-dropzone';
import { Upload, FileCode2, ArrowLeft, Clock, CheckCircle, XCircle, Loader } from 'lucide-react';

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [uploading, setUploading] = useState(false);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    Promise.all([
      projectApi.get(Number(id)),
      submissionApi.listByProject(Number(id)),
    ]).then(([pRes, sRes]) => {
      setProject(pRes.data.data);
      setSubmissions(sRes.data.data.content);
    }).catch(() => toast.error('Failed to load project'))
      .finally(() => setLoading(false));
  }, [id]);

  const onDrop = useCallback(async (acceptedFiles: File[]) => {
    if (!id) return;
    for (const file of acceptedFiles) {
      setUploading(true);
      try {
        const res = await submissionApi.upload(Number(id), file);
        toast.success(`${file.name} uploaded!`);
        setSubmissions((prev) => [res.data.data, ...prev]);
      } catch (err: any) {
        toast.error(err.response?.data?.message || `Failed to upload ${file.name}`);
      } finally {
        setUploading(false);
      }
    }
  }, [id]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/plain': ['.java', '.py', '.js', '.ts', '.cpp', '.c', '.h', '.hpp', '.go', '.rs', '.rb', '.php', '.swift', '.kt'],
    },
    maxSize: 10 * 1024 * 1024,
  });

  const statusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED': return <CheckCircle className="w-4 h-4 text-emerald-400" />;
      case 'FAILED': return <XCircle className="w-4 h-4 text-red-400" />;
      default: return <Loader className="w-4 h-4 text-primary-400 animate-spin" />;
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center h-[60vh]">
      <div className="w-10 h-10 border-3 border-primary-500/30 border-t-primary-500 rounded-full animate-spin" />
    </div>
  );

  return (
    <div className="space-y-8">
      <div className="flex items-center gap-4">
        <button onClick={() => navigate('/projects')} className="p-2 rounded-xl hover:bg-white/10 transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-3xl font-bold">{project?.name}</h1>
          <p className="text-surface-300/60 mt-1">{project?.description || 'No description'}</p>
        </div>
      </div>

      {/* Upload Zone */}
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <div {...getRootProps()}
        className={`glass-card p-10 text-center cursor-pointer transition-all duration-300 border-2 border-dashed ${
          isDragActive ? 'border-primary-500 bg-primary-500/10' : 'border-white/10 hover:border-primary-500/30 hover:bg-white/5'
        }`}>
        <input {...getInputProps()} />
        <Upload className={`w-12 h-12 mx-auto mb-4 ${isDragActive ? 'text-primary-400' : 'text-surface-300/30'}`} />
        {uploading ? (
          <div className="flex items-center justify-center gap-3">
            <div className="w-5 h-5 border-2 border-primary-500/30 border-t-primary-500 rounded-full animate-spin" />
            <p className="text-surface-300">Uploading & processing...</p>
          </div>
        ) : isDragActive ? (
          <p className="text-primary-400 font-medium">Drop files here to upload</p>
        ) : (
          <>
            <p className="text-surface-300 font-medium">Drag & drop source files here, or click to browse</p>
            <p className="text-xs text-surface-300/40 mt-2">Supported: .java, .py, .js, .ts, .cpp, .c, .go, .rs, .rb, .php, .swift, .kt (Max 10MB)</p>
          </>
        )}
      </div>
      </motion.div>

      {/* Submissions */}
      <div>
        <h2 className="text-xl font-semibold mb-4">Submissions ({submissions.length})</h2>
        {submissions.length === 0 ? (
          <div className="text-center py-16 glass-card">
            <FileCode2 className="w-12 h-12 text-surface-300/20 mx-auto mb-3" />
            <p className="text-surface-300/40">No submissions yet. Upload your first file above.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {submissions.map((sub, i) => (
              <motion.div key={sub.id} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                onClick={() => navigate(`/submissions/${sub.id}`)}
                className="glass-card-hover p-5 cursor-pointer flex items-center justify-between">
                <div className="flex items-center gap-4">
                  {statusIcon(sub.status)}
                  <div>
                    <p className="font-medium">{sub.originalFileName}</p>
                    <div className="flex items-center gap-3 mt-1">
                      <span className="text-xs text-surface-300/50">{sub.language}</span>
                      <span className="text-xs text-surface-300/30">{(sub.fileSize / 1024).toFixed(1)} KB</span>
                      <span className="text-xs text-surface-300/30 flex items-center gap-1">
                        <Clock className="w-3 h-3" /> {new Date(sub.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  {sub.status !== 'COMPLETED' && sub.status !== 'FAILED' && (
                    <div className="w-24">
                      <div className="flex justify-between text-xs text-surface-300/50 mb-1">
                        <span>{sub.currentStage}</span>
                        <span>{sub.progressPercent}%</span>
                      </div>
                      <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
                        <div className="h-full bg-primary-500 rounded-full transition-all" style={{ width: `${sub.progressPercent}%` }} />
                      </div>
                    </div>
                  )}
                  {sub.review && (
                    <span className={`text-lg font-bold ${
                      sub.review.overallScore >= 80 ? 'text-emerald-400' : sub.review.overallScore >= 50 ? 'text-yellow-400' : 'text-red-400'
                    }`}>{sub.review.overallScore}</span>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
