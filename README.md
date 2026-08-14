# Auto Clicker (for your mom's Android phone)

A small Android app that shows a floating on-screen control with GREEN and RED
buttons. Pressing GREEN taps two points on the screen (Point A, then Point B)
170 times in a row. Pressing RED does the same 230 times. A STOP button appears
while it's running so it can be cancelled early.

Written in **Kotlin**, built with **Android Studio** (Gradle project).
It uses Android's built-in Accessibility Service API to simulate taps —
this is the same mechanism real auto-clicker apps on the Play Store use.

## What's in this folder
A complete Android Studio project. You don't run/compile this on a phone
directly — you build it into an APK using Android Studio, then install
that APK on your mom's phone.

## How to build it — Option A: let GitHub build it for you (no software to install)

This project includes a `.github/workflows/build.yml` file that tells GitHub
to compile the APK automatically in the cloud. You just need a free GitHub
account and a web browser — nothing to install on your computer.

1. Go to github.com and sign up for a free account if you don't have one.
2. Click the "+" in the top right → "New repository." Name it anything
   (e.g. `mom-auto-clicker`), keep it **Public** (so the download link
   works without logging in), and click "Create repository."
3. On the new repo's page, click "uploading an existing file" (or
   "Add file" → "Upload files"). Drag the entire contents of this
   `AutoClicker` folder (including the hidden `.github` folder) into the
   browser window, then click "Commit changes." (If your browser hides the
   `.github` folder when dragging, use GitHub Desktop or the "Add file"
   button to upload `.github/workflows/build.yml` separately into the
   right path.)
4. Click the "Actions" tab at the top of the repo. You'll see a build
   running — wait about 1–2 minutes for the green checkmark.
5. To get a clean download link: go to the repo's main page → click
   "Releases" (right sidebar) → "Create a new release" → in the "tag"
   field type `v1` → click "Publish release." This triggers another build
   and, once it finishes (check the Actions tab), the release page will
   have `AutoClicker.apk` attached with a direct download link, e.g.:
   `https://github.com/<your-username>/<repo-name>/releases/download/v1/AutoClicker.apk`
6. Send that link to your mom's phone (text, WhatsApp, email — whatever's
   easiest). She taps it in her phone's browser to download and install.

## How to build it — Option B: build it yourself with Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (free) on
   a Windows/Mac/Linux computer.
2. Open Android Studio → "Open" → select this `AutoClicker` folder.
3. Let Gradle sync (first time takes a few minutes, needs internet).
4. Build → Build Bundle(s)/APK(s) → Build APK(s), or plug the phone in via
   USB and press the green ▶ Run button to install it directly.

## Installing on her phone (after downloading via the link)

When she taps the download link, her phone downloads `AutoClicker.apk`.
Opening it will show a one-time warning like "install unknown apps" or
"blocked by Play Protect" — this is normal for any app not from the Play
Store. She (or you, if you're setting it up for her) taps "Settings" in
that prompt, allows her browser to install unknown apps, goes back, and
taps install. This is a one-time step.

## How your mom uses it

1. Open the "Auto Clicker" app once. Tap **"Open Accessibility Settings."**
2. In the list that appears, find **Auto Clicker** and turn it ON. Android
   will show a warning about what accessibility services can do — this is
   normal for any app that taps the screen for you; confirm to enable it.
3. Go back to whatever app/screen she wants to tap in. A small floating
   panel with two dots (**A** and **B**) and GREEN/RED/STOP buttons will
   appear on top of everything.
4. **Drag dot A** to the exact spot that should be tapped first (roughly
   the middle-left of the screen). **Drag dot B** to the second spot
   (roughly the top-left). This only needs to be done once — the
   positions are remembered.
5. Tap **GREEN** to run the sequence (A, then B) 170 times, or **RED** for
   230 times.
6. Tap **STOP** any time to cancel early.
7. Drag the "≡ Auto Clicker" bar at the top of the panel to reposition the
   whole control if it's in the way.

## Notes
- This only taps the screen of the phone it's installed on — nothing is
  sent anywhere or accessed on any other device or account.
- Some apps/games explicitly forbid automated taps in their terms of
  service (this is common for banking apps, some games, etc.) — worth
  keeping in mind depending on what she plans to use this for.
- If Android ever kills the floating overlay (e.g., after a phone
  restart), just reopen the Auto Clicker app once to bring it back — the
  accessibility toggle stays on.
