<script setup lang="ts">
import { defineAsyncComponent, onMounted } from "vue";
import { listOfPhases } from "@/types/types";
import { useConfigStore } from "@/stores/config";
import { generateClickableLink, readableTimestamp, simpleDate } from "@/utils/utils";
import ConfigSection from "@/components/config/ConfigSection.vue";
import { isLastDateWithinXDays } from "@/utils/utils";

// Lazy Load Editor Components
const HolidayConfigEditor = defineAsyncComponent(
  () => import("@/components/config/HolidayConfigEditor.vue"),
);
const BannerConfigEditor = defineAsyncComponent(
  () => import("@/components/config/BannerConfigEditor.vue"),
);
const LivePhaseConfigEditor = defineAsyncComponent(
  () => import("@/components/config/LivePhaseConfigEditor.vue"),
);
const PenaltyConfigEditor = defineAsyncComponent(
  () => import("@/components/config/PenaltyConfigEditor.vue"),
);
const CourseConfigEditor = defineAsyncComponent(
  () => import("@/components/config/CourseConfigEditor.vue"),
);
const ScheduleShutdownEditor = defineAsyncComponent(
  () => import("@/components/config/ScheduleShutdownEditor.vue"),
);

const config = useConfigStore();

const holidayWarning = (): boolean => {
  if (config.admin.holidays.length == 0) return true;

  return isLastDateWithinXDays([...config.admin.holidays], 30);
};

onMounted(async () => {
  await useConfigStore().updateConfig();
});
</script>

<template>
  <div id="configContainer">
    <ConfigSection
      title="Banner Message"
      description="A dynamic message displayed across the top of the Autograder"
    >
      <template #editor="{ closeEditor }">
        <BannerConfigEditor :closeEditor="closeEditor" />
      </template>
      <template #current>
        <div v-if="config.public.banner.message">
          <p>
            <span class="infoLabel">Message: </span><span v-text="config.public.banner.message" />
          </p>
          <p>
            <span class="infoLabel">Link: </span>
            <span
              v-if="config.public.banner.link"
              v-html="generateClickableLink(config.public.banner.link)"
            />
            <span v-else>none</span>
          </p>
          <p>
            <span class="infoLabel">Expires: </span
            ><span v-text="readableTimestamp(config.public.banner.expiration)" />
          </p>
        </div>
        <p v-else>There is currently no banner message</p>
      </template>
    </ConfigSection>

    <ConfigSection
      title="Live Phases"
      description="These phases are live and open for students to submit to"
    >
      <template #editor="{ closeEditor }">
        <LivePhaseConfigEditor :closeEditor="closeEditor" />
      </template>
      <template #current>
        <div v-for="phase in listOfPhases()">
          <p>
            <i
              v-if="config.public.livePhases.includes(phase.toString())"
              class="fa-solid fa-circle-check"
              style="color: green"
            />
            <i v-else class="fa-solid fa-x" style="color: red" />
            {{ phase }}
          </p>
        </div>
      </template>
    </ConfigSection>

    <ConfigSection
      title="Schedule Shutdown"
      description="Schedule a time for all graded phases to be deactivated for the end of the semester, per University Policy"
    >
      <template #editor="{ closeEditor }">
        <ScheduleShutdownEditor :close-editor="closeEditor" />
      </template>
      <template #current>
        <p>
          <span class="infoLabel">Scheduled to shutdown: </span>
          {{ readableTimestamp(config.public.shutdown.timestamp) }}
        </p>
        <p v-if="config.public.shutdown.timestamp != 'never'">
          <span class="infoLabel">Warning duration: </span>
          {{ config.public.shutdown.warningMilliseconds / (60 * 60 * 1000) }} hours
        </p>
      </template>
    </ConfigSection>

    <ConfigSection
      title="Holidays"
      description="Days the Autograder should not count towards the late penalty"
    >
      <template #editor="{ closeEditor }">
        <HolidayConfigEditor :closeEditor="closeEditor" />
      </template>
      <template #current>
        <div v-if="holidayWarning()">
          <b style="background-color: red; color: white; border-radius: 5px; padding: 5px"
            >Holidays are about to run out!</b
          >
          <p><em>Please add more holidays prompty, using the University Academic Calendar.</em></p>
        </div>
        <p v-for="holiday in config.admin.holidays">
          {{ simpleDate(holiday) }}
        </p>
      </template>
    </ConfigSection>

    <ConfigSection title="Penalties" description="Values used for calculating penalties">
      <template #editor="{ closeEditor }">
        <PenaltyConfigEditor :closeEditor="closeEditor" />
      </template>
      <template #current>
        <p>
          <span class="infoLabel">Late Penalty: </span
          >{{ Math.round(config.admin.penalty.perDayLatePenalty * 100) }}%
        </p>
        <p>
          <span class="infoLabel">Max Days Penalized: </span
          >{{ config.admin.penalty.maxLateDaysPenalized }} days
        </p>

        <p>
          <span class="infoLabel">Git Commit Penalty: </span
          >{{ Math.round(config.admin.penalty.gitCommitPenalty * 100) }}%
        </p>
        <p>
          <span class="infoLabel">Lines Per Commit: </span
          >{{ config.admin.penalty.linesChangedPerCommit }} lines
        </p>
        <p>
          <span class="infoLabel">Clock Forgiveness: </span
          >{{ config.admin.penalty.clockForgivenessMinutes }} minutes
        </p>
      </template>
    </ConfigSection>

    <ConfigSection title="Courses" description="Manage Canvas course connections">
      <template #editor="{ closeEditor }">
        <CourseConfigEditor :closeEditor="closeEditor" />
      </template>
      <template #current>
        <p><span class="infoLabel">Course ID: </span>{{ config.admin.courseNumber }}</p>
      </template>
    </ConfigSection>
  </div>
</template>

<style scoped>
:deep(input) {
  border: 1px solid #ccc;
  padding: 8px;
  border-radius: 4px;
}
:deep(input[type="text"]) {
  width: 100%;
}

.infoLabel {
  font-weight: bold;
}

#configContainer {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr; /* Three columns of 100px each */
  grid-gap: 10px; /* Gap between grid items */
  margin: 10px;
}

input[type="text"] {
  padding: 5px;
  width: 100%;
}
</style>
