# Reposcorer

This simple application determines the popularity scores of repositories on Github.

The repositories are retrieved from Github by using the [Github REST API](https://docs.github.com/en/rest/search/search?apiVersion=2022-11-28#search-repositories). Simply choose the programming language and a date representing the earliest creation date, and the application will show the repositories matching the criteria, each with its popularity score.

The application uses [MinMax Scaling](https://databasecamp.de/en/ml/minmax-scaler-en) to determine the popularity score, based on:
- Number of stars
- Number of forks
- Recency of updates.

## How to run the application

Simply execute the main method using your favorite IDE (I used IntelliJ IDEA 2024.3 for development), and write the search criteria in the console prompt.

> "The root of many software problems is trying to make a simple Code Challenge application runnable in a lot of different platforms" - Albert Einstein, circa 2024

The application can also run on console, with some minimal changes (not favored due to time constraints). For example, the [Maven JAR plugin](https://maven.apache.org/plugins/maven-jar-plugin/) can be used to build an executable jar (minor changes in the pom, [as per in this link](https://www.sohamkamani.com/java/cli-app-with-maven/#running-our-code).

## Design Considerations

- Currently, the application interacts with the Github API in an unauthenticated way. According to [the API documentation](https://docs.github.com/en/rest/search/search?apiVersion=2022-11-28#rate-limit), this has a considerable request limit. As this limit can be increased by using authenticated traffic, I included in the code the possibility of adding an authentication token; this will allow bigger searches.

- The auth token mentioned before and the basic Github API data are stored in an AppConfig object. Having hardcoded values is not ideal, but this approach has been favored to manage development time efficiently. Ideally, configuration files can be present, or the sensitive data can be stored in a vault like the [HCP Vault](https://www.vaultproject.io/).

- The prompt logic to ask for the search requirements and show them on console, it's just meant to exemplify the application usage. The most important consideration here is: the popularity score is abstracted, so in case we have to expose it differently, changes will be minor. For example: we can expose a REST API to provide popularity scores easily.

- The project has basic logging support, by logging to console and a file. The focus is on the Github API interactions, as these tend to be the most useful when troubleshooting.

## Improvements

- In the case of deciding to authenticate to Github, the next step can be to look for a library that helps with Github Authentication. This will avoid adding the authentication token manually. However, this will also make the usage of this application harder, as valid credentials will be required (for exmaple, from a Github App).