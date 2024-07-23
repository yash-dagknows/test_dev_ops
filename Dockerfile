#FROM - Image to start building on.
FROM ubuntu:20.04

#MAINTAINER - Identifies the maintainer of the dockerfile.
MAINTAINER yash@dagknows.com

#RUN - Runs a command in the container
RUN echo "Hello world" > /tmp/hello_world.txt

#CMD - Identifies the command that should be used by default when running the image as a container.
CMD ["cat", "/tmp/hello_world.txt"]
