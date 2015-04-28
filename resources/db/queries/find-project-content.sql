-- Find the contents of a project.
SELECT pc.*
FROM project_contents AS pc, projects AS p
WHERE pc.project_id = :project -- Identification
    AND pc.item_id = :item -- Identification.
    AND p.owner = :owner -- Access control.
    AND p.id = pc.project_id;
