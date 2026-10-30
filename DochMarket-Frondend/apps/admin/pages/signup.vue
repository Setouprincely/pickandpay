<script setup lang="ts">
// Admin accounts are invite-only: the backend rejects self-registration for
// any ADMIN-level role (AuthServiceImp throws "Staff accounts must be
// created by a Super Admin"). A self-service signup form here would always
// fail no matter what the visitor enters, so this page explains why instead
// of pretending it's possible — same approach used on Driver's signup page.
definePageMeta({ layout: false })
</script>

<template>
  <div class="signup-page">
    <div class="signup-card">
      <div class="signup-logo">
        <img src="/images/logos/Logo-01.png" alt="Doch" class="logo-img" onerror="this.style.display='none'" />
      </div>
      <h2 class="signup-title">Admin accounts are invite-only</h2>
      <p class="signup-sub">
        You can't create a dashboard account yourself. A Super Admin needs to invite you —
        once they do, you'll get an email with a code to activate your account.
      </p>
      <NuxtLink to="/login" class="signup-btn signup-btn--link">Back to sign in</NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.signup-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 1.5rem; }
.signup-card { background: white; border-radius: 16px; padding: 40px; width: 100%; max-width: 420px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); text-align: center; }
.signup-logo { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; gap: 8px; }
.logo-img { height: 48px; }
.signup-title { font-size: 20px; font-weight: 700; color: #111827; margin: 0 0 12px; }
.signup-sub { font-size: 14px; color: #6B7280; line-height: 1.6; margin: 0 0 28px; }
.signup-btn--link { display: inline-block; background: #EF2A39; color: #fff; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-size: 14px; font-weight: 600; }
.signup-btn--link:hover { background: #d92330; }
</style>
