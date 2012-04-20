package com.pathdependent

/**
 * Package for collecting GitHub data using the Eclipse library for GitHub's 
 * v3 API.
 */
package object github {
  /** 
   * Used for seeding the collector. Some users are famous (or infamous), while
   * some are associated with one a top project in a specific langauge. I tried
   * to include several langauges, so that the initial sample would not
   * be isolated to a linguistic clique. 
   */
  val ProminentUsers = List(
    "zedshaw",      // yea...
    "cakephp",      // cakephp
    "bartaz",       // impress.js
    "mxcl",         // homebrew
    "dhh",          // rails
    "mojombo",      // jekyll...oh, and, you know...github
    "robbyrussell", // oh-my-zsh
    "nathanmarz",   // storm
    "torvalds",     // linux
    "sitaramc",     // gitolite
    "TTimo",        // DOOM!
    "rsms",         // kod
    "jboner",       // akka
    "hadley",       // a bunch of R stuff
    "ghc",          // ghc
    "daoudclarke"   // fortran code
  )
}

