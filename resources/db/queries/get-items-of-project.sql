-- Finds the items that belong, on some level, to a project
-- that belongs to a user.
WITH RECURSIVE parents(project_id) -- all the parents.
  AS (SELECT id AS project_id -- the project itself.
    FROM projects
    WHERE id = :project
      AND owner = :owner
  UNION ALL
    SELECT ch.id AS project_id -- and all its children.
    FROM projects AS ch, parents AS pr -- join onto the cte
    WHERE ch.parent_id = pr.project_id)
SELECT pc.*
FROM project_contents AS pc
WHERE pc.project_id IN (SELECT project_id FROM parents);
