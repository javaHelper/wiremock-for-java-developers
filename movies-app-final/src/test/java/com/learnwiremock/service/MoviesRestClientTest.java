package com.learnwiremock.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MoviesAppConstants.ADD_MOVIE_V1;
import static com.learnwiremock.constants.MoviesAppConstants.GET_ALL_MOVIES_V1;
import static com.learnwiremock.constants.MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1;
import static com.learnwiremock.constants.MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class MoviesRestClientTest {
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .globalTemplating(true)
                    .notifier(new ConsoleNotifier(true)
                    )
            ).build();

    private MoviesRestClient moviesRestClient;

    @BeforeEach
    void setUp() {
        int port = wireMockServer.getPort();
        String baseUrl = "http://localhost:" + port;
        log.info("WireMock running at: {}", baseUrl);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        moviesRestClient = new MoviesRestClient(webClient);
    }


    @Test
    void retrieveAllMovies() {
        wireMockServer.stubFor(get(urlEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("all-movies.json"))
        );

        List<Movie> movies = moviesRestClient.retrieveAllMovies();
        assertThat(movies)
                .isNotNull()
                .hasSizeGreaterThan(0);
    }


    @Test
    void retrieveAllMovies_matchesUrl() {
        wireMockServer.stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));

        List<Movie> movies = moviesRestClient.retrieveAllMovies();
        assertThat(movies)
                .isNotNull()
                .hasSizeGreaterThan(0);
    }

    @Test
    void retrieveMovieById() {
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        Movie movie = moviesRestClient.retrieveMovieById(9);
        assertThat(movie)
                .isNotNull()
                .extracting(Movie::getMovie_id, Movie::getName, Movie::getYear)
                .containsExactly(1, "Batman Begins", 2005);
    }

    @Test
    void retrieveMovieById_reponseTemplating() {
        //given
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-template.json")
                        .withTransformers("response-template")   // ✅ REQUIRED
                ));
        Integer movieId = 8;

        //when
        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        log.info("movie {}", movie);

        assertThat(movie)
                .isNotNull()
                .extracting(Movie::getMovie_id, Movie::getName, Movie::getYear)
                .containsExactly(movieId, "Batman Begins", 2005);
    }

    @Test
    void retrieveMovieById_notFound() {
        //given
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieId.json")));
        Integer movieId = 100;

        //when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(movieId));
    }

    @Test
    void retrieveMovieByName() {
        //given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        log.info("movieList {}", movieList);

        assertThat(movieList)
                .isNotNull()
                .hasSize(4)
                .extracting(Movie::getName)
                .containsExactly("The Avengers",
                        "Avengers: Age of Ultron",
                        "Avengers: Infinity War",
                        "Avengers: End Game");
    }

    @Test
    void retrieveMovieByName_responeTemplating() {
        //given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-byName-template.json")));

        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        //then
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertThat(movieList)
                .isNotNull()
                .hasSize(4)
                .extracting(Movie::getCast)
                .containsExactly(castExpected, castExpected, castExpected, castExpected);
    }

    @Test
    void retrieveMoviebyName_approach2() {
        //given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));


        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        //then
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertThat(movieList)
                .isNotNull()
                .hasSize(4)
                .extracting(Movie::getCast)
                .containsExactly(castExpected, castExpected, castExpected, castExpected);
    }

    @Test
    void retrieveMovieByName_Not_Found() {

        //given
        String movieName = "ABC";
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieName.json")));

        //when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByName(movieName));
    }

    @Test
    void retrieveMovieByYear() {
        //given
        Integer year = 2012;
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1 + "?year=" + year))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("year-template.json")));

        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByYear(year);
        assertThat(movieList)
                .isNotNull()
                .hasSize(2)
                .extracting(Movie::getName)
                .containsExactly("The Dark Knight Rises", "The Avengers");
    }

    @Test
    void retrieveMovieByYear_not_found() {
        //given
        Integer year = 1950;
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1 + "?year=" + year))
                .withQueryParam("year", equalTo(year.toString()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieyear.json")));

        //when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByYear(year));
    }

    @Test
    void addMovie() {
        //given
        Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toys Story 4")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        //when
        Movie addedMovie = moviesRestClient.addNewMovie(movie);
        System.out.println(addedMovie);

        //then
        assertNotNull(addedMovie.getMovie_id());
    }


    @Test
    void addMovie_responseTemplating() {
        //given
        Movie movie = new Movie(null,
                "Toys Story 4",
                "Tom Hanks, Tim Allen",
                2019,
                LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toys Story 4")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withTransformers("Random-Value")
                        .withTransformerParameter("id", new Random().nextDouble())
                        .withBodyFile("add-movie-template.json")));

        //when
        Movie addedMovie = moviesRestClient.addMovie(movie);
        System.out.println(addedMovie);

        //then
        assertNotNull(addedMovie.getMovie_id());
    }

    @Test
    void addMovie_responseTemplating_approach1() {
        //given
        Movie movie = new Movie(null,
                "Toys Story 4",
                "Tom Hanks, Tim Allen",
                2019,
                LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toys Story 4")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withTransformers("Random-Value")
                        .withTransformerParameter("id", new Random().nextInt())
                        .withBodyFile("add-movie-template-approach1.json")));

        //when
        Movie addedMovie = moviesRestClient.addMovie(movie);
        System.out.println(addedMovie);

        //then
        assertNotNull(addedMovie.getMovie_id());
    }


    @Test
    void addMovie_badRequest() {
        //given
        Movie movie = new Movie(null, null, "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("400-invalid-input.json")));

        //when
        String expectedErrorMessage = "Please pass all the input fields : [name]";
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.addMovie(movie), expectedErrorMessage);
    }


    @Test
    void updateMovie() {
        //given
        Integer movieId = 3;
        String cast = "ABC";
        Movie movie = new Movie(null, null, cast, null, null);
        wireMockServer.stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing(cast)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("updatemovie-template.json")));

        //when
        Movie updatedMovie = moviesRestClient.updateMovie(movieId, movie);

        //then
        assertTrue(updatedMovie.getCast().contains(cast));
    }

    @Test
    void updateMovie_notFound() {
        //given
        Integer movieId = 100;
        String cast = "ABC";
        Movie movie = new Movie(null, null, cast, null, null);
        wireMockServer.stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing(cast)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));


        //then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movieId, movie));
    }

    @Test
    void deleteMovie() {
        //given
        Movie movie = new Movie(null, "Toys Story 5", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toys Story 5")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-template.json")));
        Movie addedMovie = moviesRestClient.addMovie(movie);

        String expectedErrorMessage = "Movie Deleted Successfully";
        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(expectedErrorMessage)));

        //when
        String responseMessage = moviesRestClient.deleteMovie(addedMovie.getMovie_id().intValue());

        //then
        assertEquals(expectedErrorMessage, responseMessage);
    }

    @Test
    void deleteMovie_NotFound() {
        //given
        Integer id = 100;
        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        //then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(id));
    }

    @Test
    void deleteMovieByName() {
        //given
        Movie movie = new Movie(null,
                "Toys Story 5",
                "Tom Hanks, Tim Allen",
                2019,
                LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toys Story 5")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-template.json")));
        Movie addedMovie = moviesRestClient.addMovie(movie);

        String expectedErrorMessage = "Movie Deleted Successfully";
        wireMockServer.stubFor(delete(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo("Toys Story 5"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        //when
        String responseMessage = moviesRestClient.deleteMovieByName(addedMovie.getName());

        //then
        assertEquals(expectedErrorMessage, responseMessage);

        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Toys Story 5")))
                .withRequestBody(matchingJsonPath("$.cast", containing("Tom"))));

        wireMockServer.verify(1, deleteRequestedFor(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo("Toys Story 5")));

    }

    //@Test
    void deleteMovieByName_selectiveproxying() {
        //given
        Movie movie = new Movie(null, "Toys Story 5", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        Movie addedMovie = moviesRestClient.addMovie(movie);

        String expectedErrorMessage = "Movie Deleted Successfully";
        wireMockServer.stubFor(delete(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=Toys%20Story%205"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        //when
        String responseMessage = moviesRestClient.deleteMovieByName(addedMovie.getName());

        //then
        assertEquals(expectedErrorMessage, responseMessage);

        verify(exactly(1), deleteRequestedFor(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=Toys%20Story%205")));

    }

}
