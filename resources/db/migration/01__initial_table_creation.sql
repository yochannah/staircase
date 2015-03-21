-- Requires postgres 9.1+
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- The histories.
CREATE TABLE histories (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       VARCHAR(1024),
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT now(),
    owner       VARCHAR(1024) NOT NULL
);

-- The steps that contain the current state and the tool
-- that renders that state.
CREATE TABLE steps (
    id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(1024),
    tool  VARCHAR(1024) NOT NULL,
    stamp VARCHAR(1024) NOT NULL, -- Optional stamp for identifying when this tool was run - context dependent.
    data TEXT
);

-- Link table from histories to their steps.
-- Histories can have many steps, and steps can belong
-- in multiple histories.
CREATE TABLE history_step (
    history_id UUID NOT NULL REFERENCES histories (id) ON DELETE CASCADE,
    step_id    UUID NOT NULL REFERENCES steps (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);


-- Projects that contain items and possibly other projects.
CREATE TABLE projects (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title         TEXT NOT NULL,
    owner_id      TEXT NOT NULL,
    description   TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT now(),
    last_accessed TIMESTAMP WITH TIME ZONE DEFAULT now(),
    last_modified TIMESTAMP WITH TIME ZONE DEFAULT now(),
    parent_id     UUID REFERENCES projects (id) ON DELETE CASCADE,
    UNIQUE (owner_id, title)
);

-- The kind of thing that can be in a project.
CREATE TABLE project_contents (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES projects (id) ON DELETE CASCADE,
    item_id    TEXT NOT NULL,
    item_type  TEXT NOT NULL,
    source     TEXT NOT NULL,
    UNIQUE (item_id, item_type, project_id, source)
);

-- The InterMine services we care about.
CREATE TABLE services (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name          VARCHAR(1024),
    root          VARCHAR(1024),
    token         TEXT,
    valid_until   TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_token TEXT,
    owner         VARCHAR(1024) NOT NULL
);

CREATE TABLE toolsets (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    active        BOOLEAN DEFAUlT true,
    owner         VARCHAR(1024) NOT NULL
);

CREATE TABLE tool_config (
    name      VARCHAR(1024),
    toolset   UUID REFERENCES toolsets (id) ON DELETE CASCADE,
    idx       INTEGER, -- TODO - change queries.
    frontpage BOOLEAN,
    data      TEXT,
    UNIQUE (toolset, idx)
);

CREATE TABLE sessions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    data        TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT now(),
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL
);

