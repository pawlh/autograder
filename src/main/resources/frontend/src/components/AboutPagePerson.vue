<script setup lang="ts">
defineProps({
  lead: {
    type: Boolean,
    required: false,
  },
  name: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: false,
  },
  url: {
    type: String,
    required: true,
  },
  faIcon: {
    type: String,
    required: true,
  },
  tenure: {
    type: String,
    required: true,
  },
  contributions: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <div :id="lead ? 'lead' : ''">
    <p class="name">
      {{ name }} <a :href="url" target="_blank"><i :class="faIcon" /></a>
    </p>
    <p class="title" v-if="title">{{ title }}</p>
    <p class="tenure">{{ tenure }}</p>
    <p class="contributions">{{ contributions }}</p>
  </div>
</template>

<style scoped>
.name {
  font-weight: 1000;
  margin-top: 10px;
}

#lead .name {
  font-size: 22px;
}

#team .name {
  font-size: 18px;
}

.title {
  margin-top: 0;
  padding-top: 0;
  font-style: italic;
  font-size: 14px;
}

.tenure {
  font-size: 12px;
  font-style: italic;
}

.contributions {
  font-size: 14px;
}

i {
  color: #0047ba;
  display: inline-block;
}
</style>
