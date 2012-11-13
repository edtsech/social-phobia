# social-phobia

Update your social profiles.

## Usage

Create config file with following context:

``` yaml
# Browser
#
browser: firefox

# Logins
#
networks:

  instagram:
    login: johns
    pass: -----

  foursquare:
    login: example@example.com
    pass: -----

  github:
    login: johns
    pass: -----

  twitter:
    login: johns
    pass: -----

# Bio
#
first-name: "John"
last-name: "Smith"
location: "New York"
web: "https://about.me/john_smith"
bio: "Biography"
email: "example@example.com"
company: "Acme Company"
```

Run with:

`java -jar social-phobia-0.1.0-standalone.jar config.yml output.yml`

```
$ cat output.yml
- {instagram: ok}
- {foursquare: ok}
- {github: ok}
- {twitter: ok}
```

## License

Copyright © 2012 Edward Tsech

Distributed under the Eclipse Public License, the same as Clojure.
