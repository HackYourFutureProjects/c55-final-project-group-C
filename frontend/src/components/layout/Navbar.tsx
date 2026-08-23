import Link from "next/link";

export default function Navbar() {
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
          <Link href="/login">Log in</Link>
          <Link className="register-link" href="/register">
            Create account
          </Link>
        </div>
      </nav>
    </header>
  );
}
