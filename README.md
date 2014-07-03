prautobeans
===========

this project was written with the following goals in mind 

1. enable an alternative autobeans marshalling and proxy codebase 
    1. on serverside  
    1. use a jdk8 baseline
    1. soon gwt 2.7 client, with bytebuffers 
1. marshal a binary encoding
    1. that packs element sizes into bytes, or 32-bit ints when >254, and someday never, longs 
    1. that packs booleans into bits,
    1. that packs nulls into bits (or adds one bit to non-null nullables)
    1. that packs a DAG of proto defined objects
    1. DirectByteBuffer compatibility for zero-copy IO, existing proto based generators rely on heap based byte[] 
1. use maven to generate src/main/proto into the above autobeans and marshalling proxies
1. heap-averse and compact code, c-like, suitable for a c++ interop port in the near-term
 