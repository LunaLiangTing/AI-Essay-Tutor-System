<template>
  <router-view />
</template>

<script setup>
import axios from "axios";

axios.get('http://localhost:8080/').then(res => {
  console.log(res)
})
</script>
