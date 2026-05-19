import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { projectApi } from '../api/endpoints';
import type { Project } from '../types';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Plus, Search, FolderOpen, Trash2, Globe, Lock, X } from 'lucide-react';

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ name: '', description: '', language: '', isPublic: false, tags: '' });
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();

  const fetchProjects = () => {
    setLoading(true);
    projectApi.list(0, 50, search || undefined)
      .then((res) => setProjects(res.data.data.content))
      .catch(() => toast.error('Failed to load projects'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchProjects(); }, [search]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.name.trim()) { toast.error('Project name required'); return; }
    setCreating(true);
    try {
      await projectApi.create(createForm);
      toast.success('Project created!');
      setShowCreate(false);
      setCreateForm({ name: '', description: '', language: '', isPublic: false, tags: '' });
      fetchProjects();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to create project');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Delete project "${name}"?`)) return;
    try {
      await projectApi.delete(id);
      toast.success('Project deleted');
      fetchProjects();
    } catch {
      toast.error('Failed to delete project');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-white to-surface-300 bg-clip-text text-transparent">Projects</h1>
          <p className="text-surface-300/60 mt-1">Manage your code review projects</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" /> New Project
        </button>
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-surface-300/40" />
        <input type="text" value={search} onChange={(e) => setSearch(e.target.value)}
          className="input-field pl-11" placeholder="Search projects..." />
      </div>

      {/* Projects Grid */}
      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-8 h-8 border-2 border-primary-500/30 border-t-primary-500 rounded-full animate-spin" />
        </div>
      ) : projects.length === 0 ? (
        <div className="text-center py-20">
          <FolderOpen className="w-16 h-16 text-surface-300/20 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-surface-300/40">No projects yet</h3>
          <p className="text-sm text-surface-300/30 mt-1">Create your first project to start reviewing code</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {projects.map((project, i) => (
            <motion.div key={project.id} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              onClick={() => navigate(`/projects/${project.id}`)}
              className="glass-card-hover p-6 cursor-pointer group">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500/20 to-primary-600/20 border border-primary-500/20 flex items-center justify-center">
                    <FolderOpen className="w-5 h-5 text-primary-400" />
                  </div>
                  <div>
                    <h3 className="font-semibold group-hover:text-primary-400 transition-colors">{project.name}</h3>
                    {project.language && <span className="text-xs text-surface-300/50">{project.language}</span>}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {project.isPublic ? <Globe className="w-3.5 h-3.5 text-emerald-400" /> : <Lock className="w-3.5 h-3.5 text-surface-300/40" />}
                  <button onClick={(e) => { e.stopPropagation(); handleDelete(project.id, project.name); }}
                    className="p-1.5 rounded-lg hover:bg-red-500/20 text-surface-300/40 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              {project.description && <p className="text-sm text-surface-300/50 mt-3 line-clamp-2">{project.description}</p>}
              <div className="flex items-center gap-4 mt-4 pt-4 border-t border-white/5">
                <span className="text-xs text-surface-300/40">{project.submissionCount} submissions</span>
                {project.tags && (
                  <div className="flex gap-1 flex-wrap">
                    {project.tags.split(',').slice(0, 3).map((tag) => (
                      <span key={tag} className="text-[10px] px-2 py-0.5 rounded-full bg-primary-500/10 text-primary-400 border border-primary-500/20">{tag.trim()}</span>
                    ))}
                  </div>
                )}
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      <AnimatePresence>
        {showCreate && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
            onClick={() => setShowCreate(false)}>
            <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }}
              className="glass-card p-8 w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold">Create Project</h2>
                <button onClick={() => setShowCreate(false)} className="p-2 rounded-lg hover:bg-white/10"><X className="w-4 h-4" /></button>
              </div>
              <form onSubmit={handleCreate} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-surface-300 mb-1.5">Project Name *</label>
                  <input type="text" value={createForm.name} onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                    className="input-field" placeholder="My Project" required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-surface-300 mb-1.5">Description</label>
                  <textarea value={createForm.description} onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                    className="input-field resize-none h-20" placeholder="Brief description..." />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-surface-300 mb-1.5">Language</label>
                    <select value={createForm.language} onChange={(e) => setCreateForm({ ...createForm, language: e.target.value })}
                      className="input-field">
                      <option value="">Any</option>
                      <option>Java</option><option>Python</option><option>JavaScript</option>
                      <option>TypeScript</option><option>C++</option><option>C</option>
                      <option>Go</option><option>Rust</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-surface-300 mb-1.5">Tags</label>
                    <input type="text" value={createForm.tags} onChange={(e) => setCreateForm({ ...createForm, tags: e.target.value })}
                      className="input-field" placeholder="tag1, tag2" />
                  </div>
                </div>
                <label className="flex items-center gap-3 cursor-pointer">
                  <input type="checkbox" checked={createForm.isPublic}
                    onChange={(e) => setCreateForm({ ...createForm, isPublic: e.target.checked })}
                    className="w-4 h-4 rounded border-white/20 bg-white/5 text-primary-500 focus:ring-primary-500" />
                  <span className="text-sm text-surface-300">Make this project public</span>
                </label>
                <div className="flex gap-3 pt-2">
                  <button type="button" onClick={() => setShowCreate(false)} className="btn-secondary flex-1">Cancel</button>
                  <button type="submit" disabled={creating} className="btn-primary flex-1">
                    {creating ? 'Creating...' : 'Create Project'}
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
