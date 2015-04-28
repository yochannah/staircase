-- Finds the items that belong, on some level, to a user.
-- Ownership is a property of the project.
SELECT pc.*
FROM project_contents AS pc, projects AS p
WHERE p.id = pc.project_id
  AND p.owner = :owner;
