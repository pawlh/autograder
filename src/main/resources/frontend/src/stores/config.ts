import { reactive, readonly, ref } from "vue";
import { defineStore } from "pinia";
import { Phase, type RubricInfo, type RubricType } from "@/types/types";
import { getAdminConfig, getPublicConfig } from "@/services/configService";
import { useAuthStore } from "@/stores/auth";

type ImportMeta = {
  VITE_APP_BACKEND_URL: string;
};

/**
 * Config available to be read by any user
 */
export type PublicConfig = {
  banner: {
    message: string;
    link: string;
    color: string;
    expiration: string;
  };

  shutdown: {
    timestamp: string;
    warningMilliseconds: number;
  };

  livePhases: string[];
};

/**
 * Config available to be read only by admins
 */
export type PrivateConfig = {
  penalty: {
    perDayLatePenalty: number;
    gitCommitPenalty: number;
    maxLateDaysPenalized: number;
    linesChangedPerCommit: number;
    clockForgivenessMinutes: number;
  };

  courseNumber: number;
  assignments: {
    phase: Phase;
    assignmentId: number;
    rubricItems: Map<RubricType, RubricInfo>;
  }[];

  holidays: Date[];
};

// @ts-ignore
const env: ImportMeta = import.meta.env;
export const useConfigStore = defineStore("config", () => {
  const publicConfig = reactive<PublicConfig>({
    banner: {
      message: "",
      link: "",
      color: "",
      expiration: "",
    },

    shutdown: {
      timestamp: "",
      warningMilliseconds: 0,
    },

    livePhases: [],
  });

  const privateConfig = reactive<PrivateConfig>({
    penalty: {
      perDayLatePenalty: -1,
      gitCommitPenalty: -1,
      maxLateDaysPenalized: -1,
      linesChangedPerCommit: -1,
      clockForgivenessMinutes: -1,
    },
    courseNumber: -1,
    assignments: [],
    holidays: [],
  });

  const updateConfig = async () => {
    if (useAuthStore().isLoggedIn) await updateAdminConfig();
    await updatePublicConfig();
  };

  const updatePublicConfig = async () => {
    const latestPublicConfig: PublicConfig = await getPublicConfig();

    Object.assign(publicConfig, latestPublicConfig);

    // Backend lets the front end choose the default banner color
    if (!publicConfig.banner.color) publicConfig.banner.color = "#4fa0ff";
  };

  const updateAdminConfig = async () => {
    const latestAdminConfig = await getAdminConfig();

    console.log(latestAdminConfig);

    Object.assign(privateConfig, latestAdminConfig);

    console.log(privateConfig);
  };

  const backendUrl = ref<string>(env.VITE_APP_BACKEND_URL);

  return {
    updateConfig,
    updatePublicConfig,
    updateAdminConfig,
    backendUrl: readonly(backendUrl),
    public: readonly(publicConfig),
    admin: readonly(privateConfig),
  };
});
