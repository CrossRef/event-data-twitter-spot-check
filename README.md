# Event Data Twitter Spot Checker

Tool for running compliance audit on all data. Check every Event with Twitter API. Produce report of Event IDs for deleted tweets in a file. For very occasional supervised use.

## Configuration

  - `TWITTER_PASWORD`
  - `TWITTER_API_KEY`
  - `TWITTER_API_SECRET` 
  - `TWITTER_ACCESS_TOKEN`
  - `TWITTER_ACCESS_TOKEN_SECRET`
  - `QUERY_API_BASE` - http://query.eventdata.crossref.org
  - `START_CURSOR` - in case you want to pick up a stopped Query API scan

## To run

    Set environment variables, `lein run`. Deleted Event IDs will be saved in a file called `deleted-ids`.

## License

Copyright © 2017 Crossref

Distributed under the The MIT License (MIT).
