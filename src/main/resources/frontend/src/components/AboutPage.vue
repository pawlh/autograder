<script setup lang="ts">
import PopUp from "@/components/PopUp.vue";
import { onMounted, onUnmounted, ref } from "vue";
import sound from "@/assets/saints.mp3";
import InfoPanel from "@/components/InfoPanel.vue";
import AboutPagePerson from "@/components/AboutPagePerson.vue";

const audio = new Audio(sound);

const playAudio = () => {
  audio.volume = 0.5;
  audio.play();
};

const stopAudio = () => {
  audio.pause();
  audio.currentTime = 0;
};

const activated = ref<boolean>(false);

const activate = () => {
  activated.value = true;
  playAudio();
};

const deactivate = () => {
  stopAudio();
  codeIndex = 0;
  activated.value = false;
};

const secretCode = [
  "ArrowUp",
  "ArrowUp",
  "ArrowDown",
  "ArrowDown",
  "ArrowLeft",
  "ArrowRight",
  "ArrowLeft",
  "ArrowRight",
];

let codeIndex = 0;

const checkCode = (event: KeyboardEvent) => {
  if (activated.value) {
    return;
  }
  if (event.key === secretCode[codeIndex]) {
    codeIndex++;
    if (codeIndex === secretCode.length) {
      activate();
    }
  } else {
    codeIndex = 0;
  }
};

onMounted(() => {
  window.addEventListener("keydown", checkCode);
});
onUnmounted(() => {
  window.removeEventListener("keydown", checkCode);
});
</script>

<template>
  <PopUp v-if="activated" @closePopUp="deactivate">
    <div id="box">
      <h1>About the CS 240 Autograder</h1>
      <div id="leads">
        <AboutPagePerson
          name="Paul Hathaway"
          :lead="true"
          title="Father of the Autograder"
          url="https://github.com/pawlh"
          tenure="Sept 2022-Apr 2024"
          contributions="Wrote the foundations of the Autograder and its core grading functionality"
          fa-icon="fa-solid fa-otter"
        />
        <AboutPagePerson
          name="Michael Davenport"
          :lead="true"
          title="Guardian of the Autograder"
          url="https://github.com/19mdavenport"
          tenure="Jan 2023-Apr 2025"
          contributions="Developed most of the integrations with Canvas, student database handling, the code quality checker, lots of behind the scenes refactoring, and basically had a hand in every backend system in the program"
          fa-icon="fa-solid fa-hat-wizard"
        />
      </div>

      <br />

      <div id="team">
        <AboutPagePerson
          name="Dallin Webecke"
          url="https://github.com/webecke"
          tenure="Jan 2024-Apr 2025"
          contributions="Created several admin tools, cleaned up the Student UI, built a config system, and added Font Awesome"
          fa-icon="fa-solid fa-tree"
        />
        <AboutPagePerson
          name="James Finlinson"
          url="https://github.com/frozenfrank"
          tenure="Jan 2024-Apr 2025"
          contributions="Developed class-requirement enforcement systems, like late-days and git-commits"
          fa-icon="fa-solid fa-jedi"
        />
        <AboutPagePerson
          name="Than Gerlek"
          url="https://github.com/ThanGerlek"
          tenure="Jan 2024-Dec 2024"
          contributions="Refactored many backend systems to follow best programming principles"
          fa-icon="fa-solid fa-not-equal"
        />
        <AboutPagePerson
          name="Noah Pratt"
          url="https://github.com/prattnj"
          tenure="Sept 2022-Jun 2024"
          contributions="Developed several important admin analytical tools"
          fa-icon="fa-solid fa-ship"
        />
        <AboutPagePerson
          name="Isaih Barron"
          url="https://github.com/Fiwafoofa"
          tenure="Jan 2024-Aug 2024"
          contributions="Wrote some systems that did pre-compiling verification of student code"
          fa-icon="fa-solid fa-face-grin-squint-tears"
        />
        <AboutPagePerson
          name="Gavin Hunter"
          url="https://github.com/gprim"
          tenure="Sep 2024-Dec 2025"
          contributions="Introduced Prettier to the frontend development cycle"
          fa-icon="fa-solid fa-shield-cat"
        />
        <AboutPagePerson
          name="David Higueros"
          url="https://github.com/TheDavSmasher"
          tenure="Sep 2024-Dec 2025"
          contributions="Helped clean up some backend code"
          fa-icon="fa-solid fa-dragon"
        />
        <AboutPagePerson
          name="Melvin Whitaker"
          url="https://github.com/Maillman"
          tenure="Sep 2024-Dec 2025"
          contributions="Helped clean up some backend code"
          fa-icon="fa-solid fa-envelope"
        />
        <!--
        New Autograder Developers!
        Add yourself to the list right above this comment
        -->
      </div>

      <InfoPanel id="about">
        <h4>The CS 240 Autograder was created from the ground up entirely by CS 240 TAs</h4>
        <p>
          The first commit to the Autograder GitHub Repo was December 2 2023. It was developed and
          first used during the Winter 2024 semester.
        </p>
        <p>
          See the delightful mess of code we made
          <a href="https://github.com/softwareconstruction240/autograder" target="_blank"
            >here on GitHub</a
          >.
        </p>
      </InfoPanel>
    </div>
  </PopUp>
</template>

<style scoped>
* {
  text-align: center;
  text-wrap: balance;
}

#leads {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

#team {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  column-gap: 2px;
}

#box {
  max-width: 60vw;
}

PopUp {
  min-height: 100vh !important;
}
</style>
