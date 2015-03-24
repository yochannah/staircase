-- Finds the items that belong, on some level, to a project
-- that belongs to a user.
WITH RECURSIVE parents(project_id) AS -- all the parents.
    SELECT id AS project_id -- the project itself.
    FROM projects
    WHERE parent_id = :project
      AND owner = :owner
  UNION ALL
    SELECT ch.id AS project_id -- and all its children.
    FROM projects AS ch, parents AS pr
    WHERE ch.parent_id = pr.project_id
SELECT *
FROM project_contents 
WHERE parent_id IN (SELECT project_id FROM parents);
