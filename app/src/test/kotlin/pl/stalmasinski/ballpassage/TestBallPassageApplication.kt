package pl.stalmasinski.ballpassage

import pl.stalmasinski.ballpassage.data.FakeUserDataRepository

/**
 * Application for Robolectric tests (see robolectric.properties): the real app with an in-memory
 * repository, so that every test starts with fresh data and can set it up directly.
 */
class TestBallPassageApplication : BallPassageApplication() {
    override val userDataRepository = FakeUserDataRepository()
}
