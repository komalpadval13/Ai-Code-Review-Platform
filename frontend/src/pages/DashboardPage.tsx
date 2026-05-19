import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { analyticsApi } from '../api/endpoints';
import type { DashboardStats } from '../types';
import { FileCode2, FolderOpen, AlertTriangle, TrendingUp, Activity, Gauge } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const cardVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: (i: number) => ({ opacity: 1, y: 0, transition: { delay: i * 0.1, duration: 0.5 } }),
};

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    analyticsApi.dashboard().then((res) => setStats(res.data.data)).catch(() => {}).finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="w-10 h-10 border-3 border-primary-500/30 border-t-primary-500 rounded-full animate-spin" />
      </div>
    );
  }

  const statCards = [
    { label: 'Total Submissions', value: stats?.totalSubmissions ?? 0, icon: FileCode2, color: 'from-blue-500 to-blue-600', shadow: 'shadow-blue-500/20' },
    { label: 'Projects', value: stats?.totalProjects ?? 0, icon: FolderOpen, color: 'from-emerald-500 to-emerald-600', shadow: 'shadow-emerald-500/20' },
    { label: 'Issues Found', value: stats?.totalIssuesFound ?? 0, icon: AlertTriangle, color: 'from-amber-500 to-amber-600', shadow: 'shadow-amber-500/20' },
    { label: 'Average Score', value: `${stats?.averageScore ?? 0}/100`, icon: TrendingUp, color: 'from-primary-500 to-primary-600', shadow: 'shadow-primary-500/20' },
    { label: 'Maintainability', value: `${stats?.averageMaintainability ?? 0}%`, icon: Gauge, color: 'from-violet-500 to-violet-600', shadow: 'shadow-violet-500/20' },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold bg-gradient-to-r from-white to-surface-300 bg-clip-text text-transparent">Dashboard</h1>
        <p className="text-surface-300/60 mt-1">Overview of your code review activity</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        {statCards.map((card, i) => (
          <motion.div key={card.label} custom={i} variants={cardVariants} initial="hidden" animate="visible"
            className={`stat-card group hover:scale-[1.02] transition-transform cursor-default`}>
            <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${card.color} flex items-center justify-center shadow-lg ${card.shadow}`}>
              <card.icon className="w-5 h-5 text-white" />
            </div>
            <p className="text-2xl font-bold mt-2">{card.value}</p>
            <p className="text-xs text-surface-300/60 uppercase tracking-wider">{card.label}</p>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Language Distribution */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }}
          className="glass-card p-6">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <Activity className="w-5 h-5 text-primary-400" /> Language Distribution
          </h3>
          {stats?.languageDistribution && Object.keys(stats.languageDistribution).length > 0 ? (
            <div className="space-y-3">
              {Object.entries(stats.languageDistribution).map(([lang, count]) => {
                const total = Object.values(stats.languageDistribution).reduce((a, b) => a + b, 0);
                const pct = total > 0 ? (count / total) * 100 : 0;
                return (
                  <div key={lang}>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-surface-200">{lang}</span>
                      <span className="text-surface-300/60">{count} files ({pct.toFixed(0)}%)</span>
                    </div>
                    <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                      <motion.div initial={{ width: 0 }} animate={{ width: `${pct}%` }} transition={{ duration: 1, delay: 0.7 }}
                        className="h-full bg-gradient-to-r from-primary-500 to-primary-400 rounded-full" />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-surface-300/40 text-sm text-center py-8">No submissions yet. Upload your first file!</p>
          )}
        </motion.div>

        {/* Recent Activity */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.6 }}
          className="glass-card p-6">
          <h3 className="text-lg font-semibold mb-4">Recent Activity</h3>
          {stats?.recentActivities && stats.recentActivities.length > 0 ? (
            <div className="space-y-2">
              {stats.recentActivities.slice(0, 8).map((act) => (
                <div key={act.submissionId}
                  onClick={() => navigate(`/submissions/${act.submissionId}`)}
                  className="flex items-center justify-between p-3 rounded-xl hover:bg-white/5 cursor-pointer transition-colors">
                  <div className="flex items-center gap-3 min-w-0">
                    <FileCode2 className="w-4 h-4 text-primary-400 flex-shrink-0" />
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{act.fileName}</p>
                      <p className="text-xs text-surface-300/50">{act.projectName}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    {act.score !== null && (
                      <span className={`text-sm font-semibold ${act.score >= 80 ? 'text-emerald-400' : act.score >= 50 ? 'text-yellow-400' : 'text-red-400'}`}>
                        {act.score}
                      </span>
                    )}
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                      act.status === 'COMPLETED' ? 'badge-success' : act.status === 'FAILED' ? 'badge-critical' : 'badge-warning'
                    }`}>{act.status}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-surface-300/40 text-sm text-center py-8">No recent activity</p>
          )}
        </motion.div>
      </div>
    </div>
  );
}
