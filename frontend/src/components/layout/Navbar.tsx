"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function Navbar() {
  const router = useRouter();
  const { user, isLoading, authError, logout } = useAuth();

  async function handleLogout() {
    await logout();
    router.push("/");
    router.refresh();
  }

  return (
    <header className="site-header">
      <nav className="navbar" aria-label="Main navigation">
        <Link className="brand" href="/">
          JobMatch
        </Link>

        <div className="nav-links">
          <Link href="/">Home</Link>
          <Link href="/jobs">Find Jobs</Link>
          <Link href="/saved">Saved</Link>
          <Link href="/profile">Profile</Link>
        </div>

        <div className="auth-links">
          {isLoading ? null : authError ? (
            <output className="nav-auth-error">Session unavailable</output>
          ) : user ? (
            <>
              <span className="nav-user">{user.name}</span>
              <button
                className="logout-button"
                type="button"
                onClick={handleLogout}
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link href="/login">Log in</Link>
              <Link className="register-link" href="/register">
                Create account
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
