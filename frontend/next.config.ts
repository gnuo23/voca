import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
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
