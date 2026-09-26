# Pull Request

## Description

<!-- What this changes, and why. -->

<!--
  If it closes an open issue, write 'Fixes #XXXX' or 'Closes #XXXX' so the issue is linked and closed
  with the merge.
-->

### Changelog

<!--
  What this PR does, item by item. Delete this section if it only fixes a bug.
-->

- [ ] <!-- First thing -->
    - [ ] <!-- A requirement of the first thing. -->
- [ ] <!-- Second thing -->

## Checklist

<!-- Put an x in each box that applies, like this: [x] -->

#### Mandatory checks

- [ ] No open PR already provides these changes.
- [ ] The change belongs in a configurable terrain generator.
- [ ] The code follows the style of the code around it. The repository has an `.editorconfig`; use an
  IDE plugin that honours it.
- [ ] Version-specific code stays inside the NMS adapter under `platforms/bukkit/nms`, and no Paper
  type leaks into `common/`.

#### Types of changes

- [ ] Bug fix
- [ ] Build system
- [ ] Documentation
- [ ] New feature
- [ ] Performance
- [ ] Refactoring
- [ ] Repository, for example the `README.md`
- [ ] Revert
- [ ] Style, for example the `.editorconfig`
- [ ] Tests

#### Compatibility

- [ ] Introduces a breaking change.
  <!--
    A breaking change is one that does not work with a previously supported feature. Changes to code
    marked @Incubating, @Preview or @Experimental, or in a package named for a pre-release state, are
    not breaking changes.
  -->
- [ ] Introduces new functionality in a backwards compatible way.
- [ ] Fixes a bug.

#### World generation

- [ ] This change affects world generation.
- [ ] I have started a server and checked the result, not only the compile.
  <!--
    A wrong NMS binding or a wrong registry call compiles cleanly and fails at server start, so a
    passing build proves nothing about generation. Say which server and build you ran.
  -->

#### Documentation

- [ ] This change needs a documentation change.
- [ ] I have made that change.

#### Testing

- [ ] I have added tests covering this change.
- [ ] `./gradlew test` passes.
  <!-- Tests are usually unnecessary for a small change, and worth having for a large one. -->

#### Licensing

<!-- To be accepted, a change must be under GPLv3. Check one: -->

- [ ] I wrote this code and am willing to release it under
  [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).
- [ ] I did not write this code, and it is in the public domain or released under
  [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html) or a compatible licence.
  <!--
    Provide evidence of this. For a compatible licence, include its text where the licence requires
    it, and add attribution either way.
  -->
