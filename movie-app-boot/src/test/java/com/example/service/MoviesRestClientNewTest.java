package com.example.service;

import com.example.dto.Movie;
import com.example.exception.MovieErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;

import static com.example.constants.MoviesAppConstants.ADD_MOVIE_V1;
import static com.example.constants.MoviesAppConstants.GET_ALL_MOVIES_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1;
import static com.example.constants.MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
public class MoviesRestClientNewTest {

    @Autowired
    MoviesRestClient moviesRestClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance().options(wireMockConfig()
                    .dynamicPort()
                    .globalTemplating(true)
                    .notifier(new ConsoleNotifier(true)))
            .build();

    @DynamicPropertySource
    static void overrideProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("movieapp.baseUrl", () -> "http://localhost:" + wireMockServer.getPort());
    }

    @Test
    public void getAllMovies() {
        wireMockServer.stubFor(get(WireMock.anyUrl())
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBodyFile("all-movies.json")));

        List<Movie> movieList = moviesRestClient.retrieveAllMovies();

        assertThat(movieList).hasSize(10);   // simple count assertion
    }

    @Test
    public void getAllMovies_matchUrlPath() {
        wireMockServer.stubFor(get(urlPathEqualTo(GET_ALL_MOVIES_V1))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));

        // when
        List<Movie> movieList = moviesRestClient.retrieveAllMovies();
        log.info("movieList : {}", movieList);
        assertThat(movieList).hasSize(10);
    }

    @Test
    void retrieveMovieById() {
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));
        Integer movieId = 1;

        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        assertThat(movie.getName()).isEqualTo("Batman Begins");
    }

    @Test
    void retrieveMovieById_withResponseTemplating() {
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-template.json")));

        Integer movieId = 200;

        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        log.info("movie : {} ", movie);
        assertThat(movie)
                .isNotNull()
                .extracting(Movie::getMovie_id, Movie::getName, Movie::getYear)
                .containsExactly(200L, "Batman Begins", 2005);
    }

    @Test
    void retrieveMovieById_WithPriority() {
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/1"))
                .atPriority(1)
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));

        wireMockServer.stubFor(get(urlMatching("/movieservice/v1/movie/([0-9])"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie1.json")));

        Integer movieId = 1;
        Integer movieId1 = 2;

        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        Movie movie1 = moviesRestClient.retrieveMovieById(movieId1);

        assertThat(movie.getName()).isEqualTo("Batman Begins");
        assertThat(movie1.getName()).isEqualTo("Batman Begins1");
    }

    @Test
    void retrieveMovieById_NotFound() {
        //given
        Integer movieId = 100;
        wireMockServer.stubFor(get(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBodyFile("404-movieId.json")));

        //when
        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(movieId));
    }

    @Test
    void retrieveMovieByName_UrlEqualTo() {
        //given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        assertThat(movieList).isNotNull();
    }


    @Test
    void retrieveMovieByName_UrlPathEqualTo_approach2() {
        // given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", WireMock.equalTo(movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        // when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        // then
        String expectedCastName = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertEquals(4, movieList.size());
        assertEquals(expectedCastName, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByName_withResponseTemplating() {
        // given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-byname-template.json")));

        // when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        System.out.println("movieList : " + movieList);

        // then
        String expectedCastName = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertEquals(4, movieList.size());
        assertEquals(expectedCastName, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByName_urlPathEqualTo() {
        // given
        String movieName = "Avengers";
        wireMockServer.stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", WireMock.equalTo(movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        // when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        // then
        String expectedCastName = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertEquals(4, movieList.size());
        assertEquals(expectedCastName, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByYear() {
        // given
        Integer year = 2012;
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1 + "?year=" + year))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("year-template.json")));

        // when
        List<Movie> movieList = moviesRestClient.retrieveMovieByYear(year);

        // then
        assertEquals(2, movieList.size());
    }

    @Test
    void retrieveMovieByYear_Not_Found() {
        // given
        Integer year = 1950;
        wireMockServer.stubFor(get(urlEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1 + "?year=" + year))
                .withQueryParam("year", WireMock.equalTo(year.toString()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieyear.json")));

        // when & then
        assertThrows(MovieErrorResponse.class,
                () -> moviesRestClient.retrieveMovieByYear(year));
    }


    @Test
    void addNewMovie() {
        // given
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 4", 2019, batmanBeginsCrew, LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name"))
                .withRequestBody(matchingJsonPath("$.cast"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        // when
        Movie movie = moviesRestClient.addNewMovie(toyStory);

        // then
        assertTrue(movie.getMovie_id() != null);
    }


    @Test
    void addNewMovie_matchingJsonAttributeValue() throws JsonProcessingException {
        // given
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 4", 2019, batmanBeginsCrew, LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", WireMock.equalTo("Toy Story 4")))
                .withRequestBody(matchingJsonPath("$.cast", WireMock.containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        // when
        Movie movie = moviesRestClient.addNewMovie(toyStory);

        // then
        assertTrue(movie.getMovie_id() != null);
    }


    @Test
    void addNewMovie_dynamicResponse() {
        // given
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 4", 2019, batmanBeginsCrew, LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name"))
                .withRequestBody(matchingJsonPath("$.cast"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-template.json")));

        // when
        Movie movie = moviesRestClient.addNewMovie(toyStory);
        System.out.println("movie : " + movie);

        // then
        assertTrue(movie.getMovie_id() != null);
    }


    @Test
    @DisplayName("Passing the Movie name and year as Null")
    void addNewMovie_InvlaidInput() {
        // given
        String batmanBeginsCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, null, null, batmanBeginsCrew, LocalDate.of(2019, 06, 20));

        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("addmovie-invalidinput.json")));  // Removed duplicate .withBodyFile

        // when & then
        assertThrows(MovieErrorResponse.class,
                () -> moviesRestClient.addNewMovie(toyStory));
    }

    @Test
    void updateMovie() {
        // given
        String darkNightRisesCrew = "Tom Hardy";
        Movie darkNightRises = new Movie(null, null, null, darkNightRisesCrew, null);
        Integer movieId = 3;

        wireMockServer.stubFor(put(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .withRequestBody(matchingJsonPath("$.cast", WireMock.equalTo(darkNightRisesCrew)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("updatemovie-template.json")));

        // when
        Movie updatedMovie = moviesRestClient.updateMovie(movieId, darkNightRises);

        // then
        String updatedCastName = "Christian Bale, Heath Ledger , Michael Caine, Tom Hardy";
        assertTrue(updatedMovie.getCast().contains(darkNightRisesCrew));
    }


    @Test
    void updateMovie_Not_Found() {
        String darkNightRisesCrew = "Tom Hardy";
        Movie darkNightRises = new Movie(null, null, null, darkNightRisesCrew, null);
        Integer movieId = 100;

        wireMockServer.stubFor(put(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .withRequestBody(matchingJsonPath("$.cast", WireMock.equalTo(darkNightRisesCrew)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        assertThrows(MovieErrorResponse.class,
                () -> moviesRestClient.updateMovie(movieId, darkNightRises));
    }

    @Test
    void deleteMovie() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", WireMock.equalTo("Toy Story 4")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("Movie Deleted Successfully")));

        String toyStoryCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 4", 2019, toyStoryCrew, LocalDate.of(2019, 06, 20));
        Movie movie = moviesRestClient.addNewMovie(toyStory);
        Integer movieId = movie.getMovie_id().intValue();

        // when
        String response = moviesRestClient.deleteMovieById(movieId);

        // then
        String expectedResponse = "Movie Deleted Successfully";
        assertEquals(expectedResponse, response);
    }

    @Test
    void deleteMovie_notFound() {
        // given
        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movie/([0-9]+)"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));

        Integer movieId = 100;

        // when & then
        assertThrows(MovieErrorResponse.class,
                () -> moviesRestClient.deleteMovieById(movieId));
    }

    @Test
    void deleteMovieByName() {
        // given
        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", WireMock.equalTo("Toy Story 5")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movieName/.*"))
                .willReturn(WireMock.ok()));  // Or use .withBody("Movie Deleted SuccessFully") if client expects that message

        String toyStoryCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 5", 2019, toyStoryCrew, LocalDate.of(2019, 06, 20));
        Movie movie = moviesRestClient.addNewMovie(toyStory);

        // when
        String responseMessage = moviesRestClient.deleteMovieByName(movie.getName());

        // then
        assertEquals("Movie Deleted SuccessFully", responseMessage); // Ensure the response matches client expectation

        wireMockServer.verify(postRequestedFor(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", WireMock.equalTo("Toy Story 5"))));
        wireMockServer.verify(deleteRequestedFor(urlPathMatching("/movieservice/v1/movieName/.*")));
    }


    @Test
    void deleteMovieByName_usingSelectiveProxy() {
        // given
        // Stub the POST request to add a movie (returns 201 with a movie ID)
        wireMockServer.stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath("$.name", WireMock.equalTo("Toy Story 5")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"movie_id\":100,\"name\":\"Toy Story 5\",\"year\":2019,\"cast\":\"Tom Hanks, Tim Allen\",\"release_date\":\"2019-06-20\"}")));

        // Stub the DELETE request
        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movieName/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody("Movie Deleted SuccessFully")));

        String toyStoryCrew = "Tom Hanks, Tim Allen";
        Movie toyStory = new Movie(null, "Toy Story 5", 2019, toyStoryCrew, LocalDate.of(2019, 06, 20));
        Movie movie = moviesRestClient.addNewMovie(toyStory);

        // when
        String responseMessage = moviesRestClient.deleteMovieByName(movie.getName());

        // then
        assertEquals("Movie Deleted SuccessFully", responseMessage);

        wireMockServer.verify(deleteRequestedFor(urlPathMatching("/movieservice/v1/movieName/.*")));
    }

    @Test
    void deleteMovieByName_NotFound() {
        // given
        String movieName = "ABC";
        wireMockServer.stubFor(delete(urlPathMatching("/movieservice/v1/movieName/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())   // 404 – correct for “Not Found”
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("Movie not found")));            // optional body

        // when & then
        assertThrows(MovieErrorResponse.class,
                () -> moviesRestClient.deleteMovieByName(movieName));
    }
}