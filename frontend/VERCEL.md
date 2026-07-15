# Deploy the frontend to Vercel

Import `gnuo23/voca` from the Vercel dashboard and use these project settings:

- Framework Preset: `Next.js`
- Root Directory: `frontend`
- Build Command: `npm run build` (default)
- Output Directory: Next.js default (leave blank)
- Install Command: `npm install` (default)

Add the following environment variable for Production and Preview:

```text
NEXT_PUBLIC_API_BASE_URL=https://api.52-220-241-27.sslip.io
```

The backend URL must be public HTTPS; `http://localhost:8080` only works during
local development. After the first deployment, add the Vercel production origin
to the backend environment and restart the backend:

```text
APP_CORS_ALLOWED_ORIGINS=https://dungne-qdwlohqyp-anhdungletran123-9502s-projects.vercel.app
```

Multiple origins are comma-separated. For example:

```text
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://your-project.vercel.app
```

Every push to the production branch will trigger a new production deployment.
Pull requests and other branches will receive preview deployments.
