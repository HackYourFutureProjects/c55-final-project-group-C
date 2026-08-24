"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { type FormEvent, Suspense, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { loginUser } from "@/lib/api";

type FormErrors = {
  email?: string;
  password?: string;
  form?: string;
};

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const oauthError = searchParams.get("error");

  function validate() {
    const nextErrors: FormErrors = {};

    if (!email.trim()) {
      nextErrors.email = "Email is required.";
    } else if (!/^\S+@\S+\.\S+$/.test(email)) {
      nextErrors.email = "Enter a valid email address.";
    }

    if (!password) {
      nextErrors.password = "Password is required.";
    }

    setErrors(nextErrors);

    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    setErrors({});

    try {
      await loginUser({
        email: email.trim().toLowerCase(),
        password,
      });
    } catch {
      setErrors({
        form: "Invalid email or password.",
      });
      setIsSubmitting(false);
      return;
    }

    await refreshUser();

    setIsSubmitting(false);
    router.push("/");
  }

  function handleGoogleLogin() {
    window.location.href = "/api/oauth2/authorization/google";
  }

  return (
    <main>
      <h1>Log in</h1>

      {oauthError === "google_link_required" ? (
        <p role="alert">
          This email already has an account. Enter your password once to link
          Google sign-in.
        </p>
      ) : null}

      {oauthError === "oauth" ? (
        <p role="alert">Google sign-in failed. Please try again.</p>
      ) : null}

      <form onSubmit={handleSubmit} noValidate>
        {errors.form ? <p role="alert">{errors.form}</p> : null}

        <div>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? "email-error" : undefined}
          />

          {errors.email ? (
            <p id="email-error" role="alert">
              {errors.email}
            </p>
          ) : null}
        </div>

        <div>
          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            aria-invalid={Boolean(errors.password)}
            aria-describedby={errors.password ? "password-error" : undefined}
          />

          {errors.password ? (
            <p id="password-error" role="alert">
              {errors.password}
            </p>
          ) : null}
        </div>

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Logging in..." : "Log in"}
        </button>
      </form>

      <button type="button" onClick={handleGoogleLogin}>
        Continue with Google
      </button>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<main>Loading login...</main>}>
      <LoginForm />
    </Suspense>
  );
}
