import { defineStore } from "pinia";
import type { User } from "@/types/types";
import { computed, ref } from "vue";
import { usersGet } from "@/services/adminService";

export type NetIdToUserMap = Record<string, User>;

export const useAdminStore = defineStore("admin", () => {
  const users = ref<User[]>([]);
  const usersByNetId = ref<NetIdToUserMap>({});

  const updateUsers = async () => {
    const latestUsers = await usersGet();
    users.value = latestUsers;

    // Also store a cache of all the users by their netId
    const byNetId: NetIdToUserMap = {};
    for (const user of latestUsers) {
      byNetId[user.netId] = user;
    }
    usersByNetId.value = byNetId;
  };

  const admins = computed(() => users.value.filter((user) => user.role === "ADMIN"));

  const students = computed(() => users.value.filter((user) => user.role === "STUDENT"));

  return { users, admins, students, updateUsers, usersByNetId };
});
