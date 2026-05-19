export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
  avatarUrl?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  language: string;
  isPublic: boolean;
  tags: string;
  ownerUsername: string;
  ownerId: number;
  submissionCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Submission {
  id: number;
  fileName: string;
  originalFileName: string;
  language: string;
  fileSize: number;
  status: string;
  currentStage: string;
  progressPercent: number;
  projectId: number;
  projectName: string;
  createdAt: string;
  completedAt: string | null;
  review: ReviewSummary | null;
  metrics: MetricsSummary | null;
  plagiarism: PlagiarismSummary | null;
}

export interface ReviewSummary {
  id: number;
  totalIssues: number;
  criticalCount: number;
  warningCount: number;
  infoCount: number;
  overallScore: number;
  summary: string;
  findings: Finding[];
}

export interface Finding {
  id: number;
  ruleId: string;
  title: string;
  description: string;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  lineNumber: number | null;
  endLineNumber: number | null;
  codeSnippet: string | null;
  recommendation: string;
  fixedCode: string | null;
  source: 'STATIC' | 'AI';
}

export interface MetricsSummary {
  linesOfCode: number;
  blankLines: number;
  commentLines: number;
  codeLines: number;
  commentRatio: number;
  cyclomaticComplexity: number;
  maintainabilityIndex: number;
  numberOfMethods: number;
  numberOfClasses: number;
  averageMethodLength: number;
  maxNestingDepth: number;
  numberOfImports: number;
}

export interface PlagiarismSummary {
  similarityPercentage: number;
  matchingSections: string;
  comparedSubmissionId: number | null;
  comparedFileName: string | null;
  flagged: boolean;
}

export interface DashboardStats {
  totalSubmissions: number;
  totalProjects: number;
  totalIssuesFound: number;
  averageScore: number;
  averageMaintainability: number;
  languageDistribution: Record<string, number>;
  severityDistribution: Record<string, number>;
  recentActivities: RecentActivity[];
}

export interface RecentActivity {
  submissionId: number;
  fileName: string;
  projectName: string;
  status: string;
  score: number | null;
  createdAt: string;
}

export interface ProgressUpdate {
  submissionId: number;
  stage: string;
  progressPercent: number;
  message: string;
  status: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
