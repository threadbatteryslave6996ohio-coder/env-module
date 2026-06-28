# env-module

Reusable Java library for typed environment variable loading.

## Structure

- Maven project
- JPMS module name: `dev.clippy.utils.envmanager`
- Java package: `dev.clippy.utils.envmanager`

## Build

```bash
mvn package
```

The built jar will be written to `target/env-module-1.0.0-SNAPSHOT.jar`.

## Use In Another Project

Install locally:

```bash
mvn install
```

Then depend on it from another Maven project:

```xml
<dependency>
    <groupId>dev.clippy.utils</groupId>
    <artifactId>env-module</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

If you use JPMS:

```java
module my.app {
    requires dev.clippy.utils.envmanager;
}
```

## Example

```java
import dev.clippy.utils.envmanager.Env;
import dev.clippy.utils.envmanager.EnvClassBuilder;
import dev.clippy.utils.envmanager.EnvFiles;
import dev.clippy.utils.envmanager.EnvOption;
import dev.clippy.utils.envmanager.EnvSchema;
import dev.clippy.utils.envmanager.EnvType;

import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws IOException {
        EnvClassBuilder builder = EnvSchema.builder();
        EnvOption<String> host = builder.required("APP_HOST", EnvType.string());
        EnvOption<Integer> port = builder.optional("APP_PORT", EnvType.integer(), 8080);
        EnvSchema schema = builder.build();

        Env env = schema.from(EnvFiles.load());
        System.out.println(env.get(host));
        System.out.println(env.get(port));
    }
}
```
