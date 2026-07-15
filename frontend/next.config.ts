import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Vercel manages its own Next.js build output. Keep the standalone bundle
  // for the existing Docker image and other self-hosted deployments.
  output: process.env.VERCEL ? undefined : "standalone",
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "chamtoeic.edu.vn"
      }
    ]
  }
};

export default nextConfig;
