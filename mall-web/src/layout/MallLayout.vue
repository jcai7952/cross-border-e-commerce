<template>
  <div>
    <header style="border-bottom:1px solid var(--line);padding:14px 0">
      <div class="container" style="display:flex;align-items:center;justify-content:space-between">
        <router-link to="/" style="font-size:22px;font-weight:800;letter-spacing:1px">KINNSHOP</router-link>
        <span style="color:var(--muted);font-size:13px">M6 里程碑实现完整商城界面</span>
      </div>
    </header>
    <main class="container" style="padding:24px 20px"><router-view /></main>
  </div>
</template>