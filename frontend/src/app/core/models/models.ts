export type MemberType = 'GLOBAL' | 'SITE' | 'FREE';
export type Role = 'ROLE_USER' | 'ROLE_ADMIN_SITE' | 'ROLE_ADMIN_GLOBAL';
export type MatchVisibility = 'PUBLIC' | 'PRIVATE';
export type MatchStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'PLAYED';

export interface AuthUser {
  token: string;
  refreshToken: string;
  matricule: string;
  firstName: string;
  lastName: string;
  memberType: MemberType;
  homeSiteId: number | null;
  adminSiteId: number | null;
  roles: Role[];
}

export interface Court {
  id: number;
  number: number;
}

export interface Site {
  id: number;
  name: string;
  address: string;
  openingTime: string;
  closingTime: string;
  courts: Court[];
}

export interface Closure {
  id: number;
  closedOn: string;
  reason: string;
  global: boolean;
}

export interface Participation {
  playerId: number;
  matricule: string;
  fullName: string;
  paid: boolean;
}

export interface Match {
  id: number;
  siteId: number;
  siteName: string;
  courtId: number;
  courtNumber: number;
  startTime: string;
  endTime: string;
  visibility: MatchVisibility;
  status: MatchStatus;
  price: number;
  organizerId: number;
  organizerName: string;
  participants: Participation[];
  paidParticipantsCount: number;
  freeSlots: number;
}

export interface Slot {
  startTime: string;
  courtId: number;
  courtNumber: number;
  match: Match | null;
}

export interface Planning {
  siteId: number;
  siteName: string;
  day: string;
  closed: boolean;
  closureReason: string | null;
  courts: Court[];
  slots: Slot[];
}

export interface Member {
  id: number;
  matricule: string;
  firstName: string;
  lastName: string;
  email: string;
  type: MemberType;
  bookingWindowDays: number;
  homeSiteId: number | null;
  homeSiteName: string | null;
  adminSiteId: number | null;
  balanceDue: number;
  bannedUntil: string | null;
  roles: Role[];
}

export interface PaymentResult {
  id: number;
  matchId: number | null;
  payerMatricule: string;
  amount: number;
  paidAt: string;
  balancePayment: boolean;
  remainingBalance: number;
}

export interface SiteStats {
  siteId: number;
  siteName: string;
  courts: number;
  matches: number;
  occupancyRate: number;
  revenue: number;
}

export interface SlotDemand {
  startTime: string;
  matches: number;
}

export interface MemberCount {
  type: MemberType;
  label: string;
  count: number;
}

export interface Unpaid {
  matchId: number;
  startTime: string;
  siteName: string;
  matricule: string;
  fullName: string;
  reason: string;
  amount: number;
}

export interface Stats {
  siteId: number | null;
  scope: 'GLOBAL' | 'SITE';
  totalMatches: number;
  publicMatches: number;
  privateMatches: number;
  cancelledMatches: number;
  playedMatches: number;
  revenue: number;
  outstanding: number;
  occupancyRate: number;
  totalMembers: number;
  membersByType: MemberCount[];
  perSite: SiteStats[];
  slotDemand: SlotDemand[];
  unpaid: Unpaid[];
}

export interface CreateMatchRequest {
  courtId: number;
  startTime: string;
  visibility: MatchVisibility;
  privatePlayersMatricules?: string[];
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
