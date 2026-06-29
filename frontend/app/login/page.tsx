import Link from "next/link";
import { LogIn } from "lucide-react";

export default function LoginPage() {
  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="brand">
          <span className="brand-mark">V</span>
          <span>Voca</span>
        </div>
        <h1 id="login-title">Welcome back</h1>
        <p>Sign in to continue your vocabulary practice.</p>
        <form>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input id="email" name="email" type="email" placeholder="you@example.com" />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" name="password" type="password" placeholder="Password" />
          </div>
          <Link className="button" href="/dashboard" aria-label="Sign in">
            <LogIn size={18} aria-hidden="true" />
            Sign in
          </Link>
        </form>
      </section>
    </main>
  );
}
