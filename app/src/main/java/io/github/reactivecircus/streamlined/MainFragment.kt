package io.github.reactivecircus.streamlined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import io.github.reactivecircus.streamlined.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMainBinding.inflate(inflater, container, false).let {
        binding = it
        it.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentContainer = view.findViewById<View>(R.id.mainNavHostFragment)
        val navController = Navigation.findNavController(fragmentContainer)

        // setup NavController with BottomNavigationView
        NavigationUI.setupWithNavController(
            binding.bottomNavigationView,
            navController
        )

        // TODO SDK bug - fragments should NOT be recreated when re-selecting item in bottom navigation view
        //  https://issuetracker.google.com/issues/80029773
    }
}
