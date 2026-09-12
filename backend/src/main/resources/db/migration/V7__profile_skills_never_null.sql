-- No skills and an empty list are the same thing - store '{}' instead of null.
-- Split from V6 so the type change and this constraint stay separate concerns.
update user_profiles set skills = '{}' where skills is null;
alter table user_profiles alter column skills set default '{}';
alter table user_profiles alter column skills set not null;
