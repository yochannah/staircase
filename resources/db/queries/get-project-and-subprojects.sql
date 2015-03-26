-- Retrieve a project and all its nested subprojects.
WITH RECURSIVE all_projects(
    id, title, description,
    created_at, last_accessed, last_modified, parent_id)
  AS (SELECT id, title, description, created_at, last_accessed, last_modified, parent_id
    FROM projects
    WHERE id = :project -- primary key.
      AND owner = :owner -- For access control.
  UNION ALL
    SELECT sp.id, sp.title, sp.description, sp.created_at, sp.last_accessed, sp.last_modified, sp.parent_id
    FROM projects AS sp, all_projects AS ap
    WHERE sp.parent_id = ap.id) -- child of relationship.
SELECT * FROM all_projects;
