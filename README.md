# ZIO Actors + Shardcake + ZIO HTTP
| Technology | Version |
|------------|---------|
| Scala      | 2.13    |
| SBT        | 1.7.1   |
| JAVA       | 17      |
| ZIO        | 2.0.2   |

This is an effort on trying to combine the usage of these two libraries:
- [zio-actors](https://zio.github.io/zio-actors/)
- [shardcake](https://devsisters.github.io/shardcake/)

The legend says:
> the power from fusing (👉👈) these two libraries will be a killer for akka persistance + sharding.

And we are here to prove that! 😈

### Run the example
The app provided combines two examples given by the libraries:
- https://zio.github.io/zio-actors/docs/overview/overview_persistence
- https://devsisters.github.io/shardcake/docs/#an-example


        $ export DOCKER_USERNAME=<username>  // e.g: johndoe
        $ export DOCKER_REGISTRY=<registry>  // e.g: docker.io
        $ sbt -Ddocker.username=$DOCKER_USERNAME -Ddocker.registry=$DOCKER_REGISTRY docker:publish
        $ kubectl apply -f infra/
        $ kubectl port-forward deployment/account-management-http 8081



### Future Work

- Add HA to the redis deployment using redis-cluster.
- Add access permissions and user management to prevent looking up accounts the user doesn't own.
