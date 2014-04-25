###
# Utilities to help determining if lines intersect or not.
#
# All points should be of the form {x,y}
#
# The algorithm for this code was picked up from:
#   http://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
###
define [], ->

  COLINEAR = 0
  CLOCKWISE = 1
  ANTICLOCKWISE = 2

  {max, min} = Math

  # Given three points, returns true if the point q lies on the segment p-r
  onSegment = (p, q, r) ->
    (q.x <= max p.x, r.x) and
      (q.x >= min p.x, r.x) and
      (q.y <= max p.y, r.y) and
      (q.y >= min p.y, r.y)
  
  # Given three points, returns:
  #  0 -> p, q, r are co-linear
  #  1 -> Clockwise
  #  2 -> Anti-clockwise
  orientation = (p, q, r) ->
    val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.x - q.y)
    if val is 0
      return COLINEAR
    else if val > 0
      return CLOCKWISE
    else
      return ANTICLOCKWISE

  # Given four points, checks whether the line segments p1-q1 and p2-q2 intersect
  intersects = (p1, q1, p2, q2) ->
    # Get the orientations needed for the general and special cases
    o1 = orientation p1, q1, p2
    o2 = orientation p1, q1, q2
    o3 = orientation p2, q2, p1
    o4 = orientation p2, q2, q1
    
    if o1 isnt o2 and o3 isnt o4
      false # General case
    else if o1 is COLINEAR and onSegment p1, p2, q1
      true
    else if o2 is COLINEAR and onSegment p1, q2, q1
      true
    else if o3 is COLINEAR and onSegment p2, p1, q2
      true
    else if o4 is COLINEAR and onSegment p2, q1, q2
      true
    else
      false

  return {intersects}

