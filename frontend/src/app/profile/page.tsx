"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useEffect, useMemo, useState } from "react";

import { useAuth } from "@/context/AuthContext";
import {
  ApiError,
  deleteCurrentUser,
  getProfile,
  updateCurrentUser,
  updateProfile,
} from "@/lib/api";

const AVAILABLE_SKILLS = [
  "React",
  "TypeScript",
  "JavaScript",
  "Next.js",
  "HTML",
  "CSS",
  "Node.js",
  "Java",
  "Spring Boot",
  "Python",
  "SQL",
  "PostgreSQL",
  "Git",
  "Docker",
  "REST API",
  "Figma",
  "Data Analysis",
  "Power BI",
  "Azure",
  "AWS",
];

const DISCIPLINES = [
  { label: "Frontend", value: "frontend" },
  { label: "Backend", value: "backend" },
  { label: "Data", value: "data" },
  { label: "DevOps", value: "devops" },
  { label: "Mobile", value: "mobile" },
  { label: "Other", value: "other" },
];

const WORK_MODES = [
  { label: "Remote", value: "remote" },
  { label: "Hybrid", value: "hybrid" },
  { label: "On-site", value: "onsite" },
];

const EXPERIENCE_OPTIONS = [
  { label: "Entry level", value: "entry" },
  { label: "Mid level", value: "mid" },
  { label: "Senior level", value: "senior" },
];

const EMPLOYMENT_TYPES = [
  { label: "Full-time", value: "full-time" },
  { label: "Part-time", value: "part-time" },
  { label: "Contract", value: "contract" },
  { label: "Internship", value: "internship" },
];

export default function ProfilePage() {
  const router = useRouter();
  const { user, isLoading, clearUser, refreshUser } = useAuth();

  const [name, setName] = useState("");
  const [isSavingAccount, setIsSavingAccount] = useState(false);
  const [accountMessage, setAccountMessage] = useState("");
  const [accountError, setAccountError] = useState("");

  const [skills, setSkills] = useState<string[]>([]);
  const [skillSearch, setSkillSearch] = useState("");
  const [discipline, setDiscipline] = useState("");
  const [preferredCity, setPreferredCity] = useState("");
  const [workMode, setWorkMode] = useState("");
  const [experienceLevel, setExperienceLevel] = useState("");
  const [employmentType, setEmploymentType] = useState("");
  const [salaryPreference, setSalaryPreference] = useState("");

  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState("");
  const [profileError, setProfileError] = useState("");

  const [showConfirmation, setShowConfirmation] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
      return;
    }

    if (user) {
      setName(user.name);
    }
  }, [isLoading, user, router]);

  useEffect(() => {
    if (!user) {
      return;
    }

    let isActive = true;

    async function loadProfile() {
      setIsLoadingProfile(true);
      setProfileError("");

      try {
        const profile = await getProfile();

        if (!isActive) {
          return;
        }

        setSkills(profile.skills ?? []);
        setDiscipline(profile.discipline ?? "");
        setPreferredCity(profile.preferredCity ?? "");
        setWorkMode(profile.workMode ?? "");
        setExperienceLevel(profile.experienceLevel ?? "");
        setEmploymentType(profile.employmentType ?? "");

        setSalaryPreference(
          profile.salaryPreference === null ||
            profile.salaryPreference === undefined
            ? ""
            : String(profile.salaryPreference),
        );
      } catch (error) {
        if (!isActive) {
          return;
        }

        if (error instanceof ApiError && error.status === 401) {
          clearUser();
          router.replace("/login");
          return;
        }

        setProfileError(
          "We could not load your job preferences. You can still edit your account information.",
        );
      } finally {
        if (isActive) {
          setIsLoadingProfile(false);
        }
      }
    }

    void loadProfile();

    return () => {
      isActive = false;
    };
  }, [user, clearUser, router]);

  const filteredSkills = useMemo(() => {
    const query = skillSearch.trim().toLowerCase();

    if (!query) {
      return [];
    }

    return AVAILABLE_SKILLS.filter(
      (skill) => skill.toLowerCase().includes(query) && !skills.includes(skill),
    ).slice(0, 6);
  }, [skillSearch, skills]);

  function addSkill(skill: string) {
    if (skills.includes(skill)) {
      return;
    }

    setSkills((currentSkills) => [...currentSkills, skill]);
    setSkillSearch("");
    setProfileMessage("");
    setProfileError("");
  }

  function removeSkill(skill: string) {
    setSkills((currentSkills) =>
      currentSkills.filter((currentSkill) => currentSkill !== skill),
    );
    setProfileMessage("");
  }

  async function handleAccountSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!user) {
      return;
    }

    setAccountMessage("");
    setAccountError("");
    setIsSavingAccount(true);

    try {
      await updateCurrentUser({
        name: name.trim(),
        email: user.email,
      });

      await refreshUser();

      setAccountMessage("Your account information has been updated.");
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        clearUser();
        router.replace("/login");
        return;
      }

      setAccountError(
        "We could not update your account information. Please try again.",
      );
    } finally {
      setIsSavingAccount(false);
    }
  }

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setProfileMessage("");
    setProfileError("");

    if (skills.length === 0) {
      setProfileError("Please select at least one skill.");
      return;
    }

    setIsSavingProfile(true);

    try {
      const savedProfile = await updateProfile({
        skills,
        discipline: discipline || null,
        preferredCity: preferredCity.trim() || null,
        workMode: workMode || null,
        experienceLevel: experienceLevel || null,
        employmentType: employmentType || null,
        salaryPreference: salaryPreference ? Number(salaryPreference) : null,
      });

      setSkills(savedProfile.skills ?? skills);
      setDiscipline(savedProfile.discipline ?? "");
      setPreferredCity(savedProfile.preferredCity ?? "");
      setWorkMode(savedProfile.workMode ?? "");
      setExperienceLevel(savedProfile.experienceLevel ?? "");
      setEmploymentType(savedProfile.employmentType ?? "");

      setSalaryPreference(
        savedProfile.salaryPreference === null ||
          savedProfile.salaryPreference === undefined
          ? ""
          : String(savedProfile.salaryPreference),
      );

      setProfileMessage("Your job preferences have been saved.");
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        clearUser();
        router.replace("/login");
        return;
      }

      setProfileError(
        "We could not save your job preferences. Please try again.",
      );
    } finally {
      setIsSavingProfile(false);
    }
  }

  async function handleDeleteAccount() {
    setDeleteError("");
    setIsDeleting(true);

    try {
      await deleteCurrentUser();

      clearUser();
      router.replace("/");
      router.refresh();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        clearUser();
        router.replace("/login");
        return;
      }

      setDeleteError("We could not delete your account. Please try again.");
      setIsDeleting(false);
    }
  }

  if (isLoading || !user) {
    return null;
  }

  return (
    <div className="profile-page">
      <header className="profile-header">
        <p className="profile-eyebrow">YOUR PROFILE</p>

        <h1>Profile</h1>

        <p>
          Manage your account information and tell JobMatch what you are looking
          for.
        </p>
      </header>

      <section className="profile-section">
        <div className="profile-section-heading">
          <p>PERSONAL INFORMATION</p>
          <h2>Edit profile</h2>
          <p>
            Keep your account information up to date. Your email address cannot
            be changed here.
          </p>
        </div>

        <form className="profile-form" onSubmit={handleAccountSubmit}>
          <div className="profile-field">
            <label htmlFor="profile-name">Name</label>

            <input
              id="profile-name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
            />
          </div>

          <div className="profile-field">
            <label htmlFor="profile-email">Email</label>

            <input
              id="profile-email"
              type="email"
              value={user.email}
              readOnly
              aria-describedby="profile-email-help"
            />

            <p className="profile-field-help" id="profile-email-help">
              Email changes require verification and are not supported yet.
            </p>
          </div>

          {accountMessage ? (
            <output className="profile-message">{accountMessage}</output>
          ) : null}

          {accountError ? (
            <p className="profile-error" role="alert">
              {accountError}
            </p>
          ) : null}

          <div className="profile-form-actions">
            <button
              className="profile-save-button"
              type="submit"
              disabled={isSavingAccount || !name.trim()}
            >
              {isSavingAccount ? "Saving..." : "Save account changes"}
            </button>
          </div>
        </form>
      </section>

      <section className="profile-section">
        <div className="profile-section-heading">
          <p>JOB PREFERENCES</p>

          <h2>What are you looking for?</h2>

          <p>
            Skills are the most important part of your profile. Other
            preferences can be left empty.
          </p>
        </div>

        {isLoadingProfile ? (
          <p className="profile-loading">Loading your preferences...</p>
        ) : (
          <form className="profile-form" onSubmit={handleProfileSubmit}>
            <div className="profile-field profile-field-full">
              <label htmlFor="skill-search">Skills *</label>

              <p className="profile-field-help">
                Choose the skills that best describe your experience.
              </p>

              {skills.length > 0 ? (
                <div className="profile-selected-skills">
                  {skills.map((skill) => (
                    <span className="profile-skill-tag" key={skill}>
                      {skill}

                      <button
                        type="button"
                        aria-label={`Remove ${skill}`}
                        onClick={() => removeSkill(skill)}
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              ) : null}

              <div className="profile-skill-picker">
                <input
                  id="skill-search"
                  type="search"
                  value={skillSearch}
                  onChange={(event) => setSkillSearch(event.target.value)}
                  placeholder="Search skills, e.g. React"
                  autoComplete="off"
                />

                {filteredSkills.length > 0 ? (
                  <div className="profile-skill-options">
                    {filteredSkills.map((skill) => (
                      <button
                        type="button"
                        key={skill}
                        onClick={() => addSkill(skill)}
                      >
                        {skill}
                      </button>
                    ))}
                  </div>
                ) : null}

                {skillSearch.trim() &&
                filteredSkills.length === 0 &&
                !skills.some(
                  (skill) =>
                    skill.toLowerCase() === skillSearch.trim().toLowerCase(),
                ) ? (
                  <p className="profile-skill-empty">
                    No supported skill found.
                  </p>
                ) : null}
              </div>
            </div>

            <div className="profile-field">
              <label htmlFor="discipline">Target role / discipline</label>

              <select
                id="discipline"
                value={discipline}
                onChange={(event) => setDiscipline(event.target.value)}
              >
                <option value="">No preference</option>

                {DISCIPLINES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="profile-field">
              <label htmlFor="preferred-city">Preferred city</label>

              <input
                id="preferred-city"
                type="text"
                value={preferredCity}
                onChange={(event) => setPreferredCity(event.target.value)}
                placeholder="e.g. Utrecht"
              />
            </div>

            <div className="profile-field">
              <label htmlFor="work-mode">Work mode</label>

              <select
                id="work-mode"
                value={workMode}
                onChange={(event) => setWorkMode(event.target.value)}
              >
                <option value="">No preference</option>

                {WORK_MODES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="profile-field">
              <label htmlFor="experience-level">Experience</label>

              <select
                id="experience-level"
                value={experienceLevel}
                onChange={(event) => setExperienceLevel(event.target.value)}
              >
                <option value="">No preference</option>

                {EXPERIENCE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="profile-field">
              <label htmlFor="employment-type">Employment type</label>

              <select
                id="employment-type"
                value={employmentType}
                onChange={(event) => setEmploymentType(event.target.value)}
              >
                <option value="">No preference</option>

                {EMPLOYMENT_TYPES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="profile-field">
              <label htmlFor="salary-preference">Salary preference</label>

              <input
                id="salary-preference"
                type="number"
                min="0"
                value={salaryPreference}
                onChange={(event) => setSalaryPreference(event.target.value)}
                placeholder="e.g. 45000"
              />
            </div>

            {profileMessage ? (
              <output className="profile-message">{profileMessage}</output>
            ) : null}

            {profileError ? (
              <p className="profile-error" role="alert">
                {profileError}
              </p>
            ) : null}

            <div className="profile-form-actions">
              <button
                className="profile-save-button"
                type="submit"
                disabled={isSavingProfile}
              >
                {isSavingProfile ? "Saving..." : "Save preferences"}
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="profile-section">
        <div className="profile-section-heading">
          <p>ACCOUNT</p>
          <h2>Account settings</h2>
        </div>

        <div className="danger-zone">
          <div className="danger-zone-copy">
            <p className="danger-zone-label">DANGER ZONE</p>

            <h3>Delete your account</h3>

            <p>
              Permanently delete your account and all data associated with it.
              This action cannot be undone.
            </p>
          </div>

          {!showConfirmation ? (
            <button
              className="danger-button"
              type="button"
              onClick={() => setShowConfirmation(true)}
            >
              Delete account
            </button>
          ) : (
            <div className="delete-confirmation">
              <p className="delete-confirmation-title">
                Are you sure you want to delete your account?
              </p>

              <p className="delete-confirmation-copy">
                Your account and associated data will be permanently removed.
              </p>

              {deleteError ? (
                <p className="delete-error" role="alert">
                  {deleteError}
                </p>
              ) : null}

              <div className="delete-confirmation-actions">
                <button
                  className="danger-button"
                  type="button"
                  onClick={handleDeleteAccount}
                  disabled={isDeleting}
                >
                  {isDeleting ? "Deleting..." : "Yes, delete my account"}
                </button>

                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => {
                    setShowConfirmation(false);
                    setDeleteError("");
                  }}
                  disabled={isDeleting}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
