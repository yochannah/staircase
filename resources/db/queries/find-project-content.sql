-- Find the contents of a project.
SELECT pc.*
FROM project_contents AS pc, projects AS p
WHERE pc.project_id = :project
    AND p.id = pc.project_id
    AND p.owner = :owner -- Access control.
    AND pc.item = :item; -- Identification.
