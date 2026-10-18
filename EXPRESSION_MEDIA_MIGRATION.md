# Expression Match media migration — staged deployment

The media pack contains 60 authored scenario mappings, 120 WebP pictures, and
60 exact-phrase MP3 clips. These are copies; frontend originals are preserved.

## Deploy first

1. Commit this seeder, its test, and `src/main/resources/expression-media-seed/`.
2. Deploy the backend. On application ready, after the question seeders run,
   `ExpressionMediaSeeder` matches level/set/phrase and both scenario captions.
3. It replaces only blank or old wave/bow picture URLs and blank audio URLs.
   Existing administrator uploads, custom URLs, captions and scoring remain intact.
4. Media is stored in MongoDB GridFS with a content-addressed seed key.
   Repeat runs reuse those files and skip question fields already populated.
5. Check Render logs for `Expression media seed complete`. If it reports an
   incomplete migration, do not remove frontend assets; resolve and rerun safely.
6. Verify both images and audio through the admin and student app for all levels.

## Do not remove bundled assets yet

The frontend change makes database URLs take precedence but retains bundled media
when no real database URL exists. A failed remote picture remains unscored using
the existing retry/caption workflow.

Only after live verification should the bundled image/audio imports be removed
and originals archived outside the frontend repository, within JAPLEARN_OFFICIAL.
Until imports are removed, this migration does not reduce OTA asset counts.

The last OTA attempt contained 1,205 assets against the server's 1,000 limit.
Removing these 180 alone would still leave approximately 1,025; at least 26 more
active bundled assets need a separately verified migration (preferably with
additional headroom). Do not remove active teacher/admin screens or audio merely
to meet the limit.

## Regenerate the deployment pack

From the frontend repository, run `node scripts/prepare_expression_media_seed.cjs`.
It verifies every source exists before copying files and generating the manifest.

To disable the startup migration, set `expression-match.media-seed.enabled=false`.

## Live pilot already verified

The canonical level-1 homecoming question (ただいま) was updated through the admin
UI, and both GridFS pictures loaded successfully. Those admin uploads are
preserved by the seeder. The other media has not yet been migrated in production.
