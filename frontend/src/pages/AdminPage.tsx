import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { adminApi } from '../api/endpoints';
import toast from 'react-hot-toast';
import { Users, FileCode2, FolderOpen, Activity, Shield, Search, ToggleLeft, ToggleRight } from 'lucide-react';

export default function AdminPage() {
  const [stats, setStats] = useState<any>(null);
  const [users, setUsers] = useState<any[]>([]);
  const [logs, setLogs] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'overview' | 'users' | 'logs'>('overview');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      adminApi.stats(),
      adminApi.users(),
      adminApi.auditLogs(),
    ]).then(([sRes, uRes, lRes]) => {
      setStats(sRes.data.data);
      setUsers(uRes.data.data.content);
      setLogs(lRes.data.data.content);
    }).catch(() => toast.error('Failed to load admin data'))
      .finally(() => setLoading(false));
  }, []);

  const handleToggleUser = async (userId: number) => {
    try {
      await adminApi.toggleUser(userId);
      toast.success('User status updated');
      const res = await adminApi.users(0, search || undefined);
      setUsers(res.data.data.content);
    } catch {
      toast.error('Failed to update user');
    }
  };

  const handleSearchUsers = async () => {
    try {
      const res = await adminApi.users(0, search || undefined);
      setUsers(res.data.data.content);
    } catch {}
  };

  useEffect(() => { handleSearchUsers(); }, [search]);

  if (loading) return (
    <div className="flex items-center justify-center h-[60vh]">
      <div className="w-10 h-10 border-3 border-primary-500/30 border-t-primary-500 rounded-full animate-spin" />
    </div>
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold bg-gradient-to-r from-white to-surface-300 bg-clip-text text-transparent flex items-center gap-3">
          <Shield className="w-8 h-8 text-primary-400" /> Admin Panel
        </h1>
        <p className="text-surface-300/60 mt-1">System management and monitoring</p>
      </div>

      {/* Tab Navigation */}
      <div className="flex gap-1 p-1 bg-white/5 rounded-xl w-fit">
        {(['overview', 'users', 'logs'] as const).map((tab) => (
          <button key={tab} onClick={() => setActiveTab(tab)}
            className={`px-5 py-2 rounded-lg text-sm font-medium transition-all ${
              activeTab === tab ? 'bg-primary-500 text-white shadow-lg' : 'text-surface-300/60 hover:text-white'
            }`}>
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            { label: 'Total Users', value: stats.totalUsers, icon: Users, color: 'from-blue-500 to-blue-600' },
            { label: 'Active Users', value: stats.activeUsers, icon: Activity, color: 'from-emerald-500 to-emerald-600' },
            { label: 'Total Submissions', value: stats.totalSubmissions, icon: FileCode2, color: 'from-primary-500 to-primary-600' },
            { label: 'Total Projects', value: stats.totalProjects, icon: FolderOpen, color: 'from-violet-500 to-violet-600' },
          ].map((card, i) => (
            <motion.div key={card.label} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }} className="stat-card">
              <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${card.color} flex items-center justify-center`}>
                <card.icon className="w-5 h-5 text-white" />
              </div>
              <p className="text-2xl font-bold mt-2">{card.value}</p>
              <p className="text-xs text-surface-300/60 uppercase">{card.label}</p>
            </motion.div>
          ))}
        </div>
      )}

      {activeTab === 'users' && (
        <div className="space-y-4">
          <div className="relative max-w-md">
            <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-surface-300/40" />
            <input type="text" value={search} onChange={(e) => setSearch(e.target.value)}
              className="input-field pl-11" placeholder="Search users..." />
          </div>
          <div className="glass-card overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-white/5">
                  <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">User</th>
                  <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">Email</th>
                  <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">Role</th>
                  <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">Status</th>
                  <th className="text-right px-6 py-3 text-xs text-surface-300/50 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary-500/20 flex items-center justify-center text-sm font-bold text-primary-400">
                          {u.username?.[0]?.toUpperCase()}
                        </div>
                        <span className="font-medium">{u.username}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-surface-300/60">{u.email}</td>
                    <td className="px-6 py-4"><span className={u.role === 'ADMIN' ? 'badge-critical' : 'badge-info'}>{u.role}</span></td>
                    <td className="px-6 py-4"><span className={u.enabled ? 'badge-success' : 'badge-critical'}>{u.enabled ? 'Active' : 'Disabled'}</span></td>
                    <td className="px-6 py-4 text-right">
                      <button onClick={() => handleToggleUser(u.id)}
                        className="p-2 rounded-lg hover:bg-white/10 transition-colors">
                        {u.enabled ? <ToggleRight className="w-5 h-5 text-emerald-400" /> : <ToggleLeft className="w-5 h-5 text-surface-300/40" />}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {activeTab === 'logs' && (
        <div className="glass-card overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-white/5">
                <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">Time</th>
                <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">User</th>
                <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">Action</th>
                <th className="text-left px-6 py-3 text-xs text-surface-300/50 uppercase">Details</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                  <td className="px-6 py-3 text-xs text-surface-300/50">{new Date(log.createdAt).toLocaleString()}</td>
                  <td className="px-6 py-3 text-sm">{log.username || '-'}</td>
                  <td className="px-6 py-3"><span className="badge-info">{log.action}</span></td>
                  <td className="px-6 py-3 text-sm text-surface-300/60 max-w-md truncate">{log.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
