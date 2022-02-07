#include <unistd.h>
 #include <fcntl.h>
#include <sys/time.h>
#include <cstdio>
#include <string>

typedef unsigned long u64;


extern "C" int find_abs (void* trie, char* key, u64 len);

#define CEDAR_NO_PATH -1
#define CEDAR_NO_VALUE -2

inline bool lookup_key (void* trie, char* key, u64 start, u64 end)
{
	return find_abs (trie,(key + start), end-start ) >= 0;
}

size_t read_data (const char* file, char*& data) {
  int fd = ::open (file, O_RDONLY);
  if (fd < 0)
    { std::fprintf (stderr, "no such file: %s\n", file); std::exit (1); }
  size_t size = static_cast <size_t> (::lseek (fd, 0L, SEEK_END));
  data = new char[size];
  ::lseek (fd, 0L, SEEK_SET);
  size_t read = ::read  (fd, data, size);
  if(read != size){
	  std::fprintf (stderr, "Expected: %zu. Read: %zu\n",size,read);
	  throw;
  }
  ::close (fd);
  return size;
}

inline char* find_sep (char* p) {
	while (*p != '\n') {
		++p;
	}
	// *p = '\0';
	return p;
}

void lookup (void* trie, char* data, size_t size, int& n_, int& n) {
  for (char* start (data), *end (data), *tail (data + size);
       end != tail; start = ++end) {
    end = find_sep (end);

    auto s = start;
    auto e = end;

    if (lookup_key (trie, data, s-data, e-data))
      ++n_;
    ++n;
  }
}

void run(void* trie,char* keys,size_t size) {
  std::fprintf (stderr, "Base: %p\n", (void*)&keys);
  struct timeval st, et;
  int n (0), n_ (0);
  ::gettimeofday (&st, NULL);
  lookup(trie, keys, size, n, n_);
  ::gettimeofday (&et, NULL);
  double elapsed = (et.tv_sec - st.tv_sec) + (et.tv_usec - st.tv_usec) * 1e-6;
  std::fprintf (stderr, "%-20s %.2f sec (%.2f nsec per key)\n","Time to search:", elapsed, elapsed * 1e9 / n);
  std::fprintf (stderr, "%-20s %d\n", "Words:", n);
  std::fprintf (stderr, "%-20s %d\n", "Found:", n_);
}

int main(int len,char** args){
    char* keys = 0;
    char* trie = 0;
    auto size = read_data ("../data/keys", keys);
    auto trie_size = read_data("../data/trie", trie);

    std::fprintf (stderr, "key_size: %zu. trie_size: %zu\n",size,trie_size);

    for(int i=0;i<5;i++){
        auto ks = keys;
    	run(static_cast<void*>(trie), ks, size);
    }
}
