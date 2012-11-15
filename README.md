# social-phobia

Update your social profiles.

## Usage

Create config file with following content:

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

`java -jar target/social-phobia-0.1.1-standalone.jar config.yml output.yml`

Last build you can get [here](https://dl.dropbox.com/u/2428018/social-phobia-0.1.1-standalone.jar).

```
$ cat output.yml
- instagram: {status: ok}
- foursquare: {status: ok}
- github: {status: ok}
- twitter: {error: #signin-email not found}
- facebook: {error: not supported}
```

## License

Copyright © 2012 Edward Tsech

Distributed under the Eclipse Public License, the same as Clojure.
