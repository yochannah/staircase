-- Update any columns in violation.
UPDATE projects
    SET title = replace(title, '/', '-')
    WHERE title LIKE '%/%';
-- Disallow / in project titles (they prevent nice URIs)
ALTER TABLE projects ADD CONSTRAINT projects_title_sluggish
      CHECK (title NOT LIKE '%/%');

-- Allow identically named projects as long as they are not
-- in the same node of the graph (including root).
ALTER TABLE projects DROP CONSTRAINT projects_owner_title_key;
CREATE UNIQUE INDEX
       project_owner_title_parent_id
       ON projects
      (owner, title, COALESCE(parent_id, uuid_nil()));
ALTER TABLE projects ADD CONSTRAINT projects_parent_not_nil
      CHECK (parent_id <> uuid_nil());
