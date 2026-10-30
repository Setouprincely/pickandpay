-- One person = one account (one email/password) that may hold several roles,
-- e.g. a seller or driver who also shops. users.user_role_id stays the primary role;
-- additional roles live here.
CREATE TABLE user_extra_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_extra_roles_role ON user_extra_roles (role_id);
