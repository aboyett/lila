package lila
package forum

import scalaz.effects._
import com.github.ornicar.paginator.Paginator

final class CategApi(env: ForumEnv) {

  val list: IO[List[CategView]] = for {
    categs ← env.categRepo.all
    views ← (categs map { categ ⇒
      env.postApi get categ.lastPostId map { post ⇒
        CategView(categ, post, env.postApi.pageOf)
      }
    }).sequence
  } yield views

  def show(slug: String, page: Int): IO[Option[(Categ, Paginator[TopicView])]] =
    env.categRepo bySlug slug map {
      _ map { categ ⇒
        categ -> env.topicApi.paginator(categ, page)
      }
    }

  def denormalize(categ: Categ): IO[Unit] = for {
    topics ← env.topicRepo byCateg categ
    nbPosts ← env.postRepo countByTopics topics
    lastPost ← env.postRepo lastByTopics topics
    _ ← env.categRepo.saveIO(categ.copy(
      nbTopics = topics.size,
      nbPosts = nbPosts,
      lastPostId = lastPost.id
    ))
  } yield ()

  val denormalize: IO[Unit] = for {
    categs ← env.categRepo.all
    _ ← categs.map(denormalize).sequence
  } yield ()
}
