import { type PrivateConfig, type PublicConfig, useConfigStore } from "@/stores/config";
import { Phase } from "@/types/types";
import { ServerCommunicator } from "@/network/ServerCommunicator";

export const getPublicConfig = async (): Promise<PublicConfig> => {
  return await ServerCommunicator.getRequest<PublicConfig>("/api/config");
};

export const getAdminConfig = async (): Promise<PrivateConfig> => {
  return await ServerCommunicator.getRequest<PrivateConfig>("/api/admin/config");
};

export const setPenalties = async (
  maxLateDaysPenalized: number,
  gitCommitPenalty: number,
  perDayLatePenalty: number,
  linesChangedPerCommit: number,
  clockForgivenessMinutes: number,
) => {
  await doSetConfigItem("/api/admin/config/penalties", {
    maxLateDaysPenalized: maxLateDaysPenalized,
    gitCommitPenalty: gitCommitPenalty,
    perDayLatePenalty: perDayLatePenalty,
    linesChangedPerCommit: linesChangedPerCommit,
    clockForgivenessMinutes: clockForgivenessMinutes,
  });
};

export const setBanner = async (
  message: String,
  link: String,
  color: String,
  expirationTimestamp: String,
): Promise<void> => {
  await doSetConfigItem("/api/admin/config/banner", {
    bannerMessage: message,
    bannerLink: link,
    bannerColor: color,
    bannerExpiration: expirationTimestamp,
  });
};

export const setLivePhases = async (phases: Array<Phase>): Promise<void> => {
  await doSetConfigItem("/api/admin/config/phases", { phases: phases });
};

export const setGraderShutdown = async (
  shutdownTimestamp: string,
  shutdownWarningHours: number,
): Promise<void> => {
  if (shutdownWarningHours < 0) shutdownWarningHours = 0;

  await doSetConfigItem("/api/admin/config/phases/shutdown", {
    shutdownTimestamp: shutdownTimestamp,
    shutdownWarningMilliseconds: Math.trunc(shutdownWarningHours * 60 * 60 * 1000), // convert to milliseconds
  });
};

export const reloadCourseIds = async (): Promise<void> => {
  await doSetConfigItem("/api/admin/config/reloadCourseIds", {});
};

export const setCourseId = async (courseNumber: number) => {
  await doSetConfigItem("/api/admin/config/courseId", { courseId: courseNumber });
};

export const updateHolidays = async (dates: string[]) => {
  await doSetConfigItem("/api/admin/config/holidays", {
    holidays: dates,
  });
};

const doSetConfigItem = async (path: string, body: Object): Promise<void> => {
  await ServerCommunicator.postRequest(path, body, false);
  await useConfigStore().updateConfig();
};
