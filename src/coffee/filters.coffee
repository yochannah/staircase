define ['angular', 'services'], (ng, services) ->

  Filters = ng.module('steps.filters', ['steps.services'])

  Filters.filter('interpolate', ['version', (v) -> (t) -> String(t).replace(/\%VERSION\%/mg, v)])

  Filters.filter 'roughDate', Array '$filter', (filters) -> (str) ->
    date = new Date str
    now = new Date()

    minutesAgo = (now.getTime() - date.getTime()) / 60 / 1000

    if minutesAgo < 1
      return "a moment ago"

    if minutesAgo < 2
      return "one minute ago"
    
    if minutesAgo < 60
      return "today, #{ minutesAgo.toFixed() } minutes ago"

    hoursAgo = minutesAgo / 60

    if hoursAgo < now.getHours()
      return "today, #{ hoursAgo.toFixed() } hours ago"

    daysAgo = hoursAgo / 24

    if hoursAgo > (1 + (now.getHours() / 24))
      return filters('date')(str)
    else
      return "yesterday"

  return Filters
